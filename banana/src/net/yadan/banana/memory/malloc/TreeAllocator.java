/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory.malloc;

import java.util.LinkedList;
import java.util.Queue;

import net.yadan.banana.memory.IBlockAllocator;
import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.MemInitializer;
import net.yadan.banana.memory.OutOfMemoryException;
import net.yadan.banana.memory.block.BlockAllocator;


//TODO: simplify
public class TreeAllocator implements IMemAllocator {

  private static final int INDEX_NUM_BLOCKS_OFFSET = 0;
  private static final int INDEX_DATA_OFFSET = 1;

  IBlockAllocator m_blocks;
  private final int m_blockSize;
  private final int m_indexBlockCapacity;

  public TreeAllocator(int maxBlocks, int blockSize) {
    this(maxBlocks, blockSize, 0, null);
  }

  public TreeAllocator(int maxBlocks, int blockSize, MemInitializer initializer) {
    this(maxBlocks, blockSize, 0, initializer);
  }

  public TreeAllocator(int maxBlocks, int blockSize, double growthFactor) {
    this(maxBlocks, blockSize, growthFactor, null);
  }

  public TreeAllocator(int maxBlocks, int blockSize, double growthFactor, MemInitializer initializer) {
    this(new BlockAllocator(maxBlocks, blockSize, growthFactor, initializer));
  }

  public TreeAllocator(IBlockAllocator blocks) {
    m_blockSize = blocks.blockSize();
    m_indexBlockCapacity = m_blockSize - INDEX_DATA_OFFSET;
    m_blocks = blocks;
  }


  @Override
  public int malloc(int size) throws OutOfMemoryException {
    if (size < 0) {
      throw new IllegalArgumentException("malloc size must be non-negative");
    }

    if (size <= m_blockSize) {
      return m_blocks.malloc();
    } else {
      if (m_blockSize <= 2) {
        throw new UnsupportedOperationException(
            "TreeIntAllocator Multi block allocation requires minimum block size of 3");
      }
      return ~multiBlockMalloc(size);
    }
  }

  private int multiBlockMalloc(int size) {
    int indexPointer = m_blocks.malloc();
    try {
      int numDataBlocks = 1 + ((size - 1) / m_blockSize); // ceil(a/b)
      setInt(indexPointer, INDEX_NUM_BLOCKS_OFFSET, numDataBlocks);

      // capacity of index that supports memory size
      int maxCapacity = maximumCapacityForNumBlocks(numDataBlocks);

      if (m_indexBlockCapacity >= numDataBlocks) {
        // allocating data
        int blockNum = 0;
        try {
          for (; blockNum < numDataBlocks; blockNum++) {
            m_blocks.setInt(indexPointer, INDEX_DATA_OFFSET + blockNum, m_blocks.malloc());
          }
        } catch (OutOfMemoryException e) {
          for (int i = 0; i < blockNum; i++) {
            int p = m_blocks.getInt(indexPointer, INDEX_DATA_OFFSET + i);
            m_blocks.free(p);
          }
          throw e;
        }
      } else {
        // allocating index
        int nextLevelCapacity = maxCapacity / m_indexBlockCapacity;
        int numIndexBlocksToAllocate = 1 + (size - 1) / nextLevelCapacity;
        int left = size;
        int indexedMemorySize = nextLevelCapacity;
        int blockNum = 0;
        try {
          for (; blockNum < numIndexBlocksToAllocate; blockNum++) {
            if (left <= nextLevelCapacity)
              indexedMemorySize = left;
            left -= indexedMemorySize;
            assert left >= 0;

            if (indexedMemorySize <= m_blockSize) {
              m_blocks.setInt(indexPointer, INDEX_DATA_OFFSET + blockNum, m_blocks.malloc());
            } else {
              int p = multiBlockMalloc(indexedMemorySize);
              m_blocks.setInt(indexPointer, INDEX_DATA_OFFSET + blockNum, ~p);
            }
          }
        } catch (OutOfMemoryException e) {
          for (int i = 0; i < blockNum; i++) {
            int p = m_blocks.getInt(indexPointer, INDEX_DATA_OFFSET + i);
            free(p);
          }
          throw e;
        }
      }

      return indexPointer;
    } catch (OutOfMemoryException e) {
      m_blocks.free(indexPointer);
      throw e;
    }
  }

  @Override
  public int realloc(int pointer, int size) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (size < 0) {
      throw new IllegalArgumentException("malloc size must be non-negative");
    }
    if (pointer > 0) {
      if (size <= m_blockSize) {
        return pointer;
      } else {
        // single block
        int ret = malloc(size);
        int p = findPointerForOffset(ret, 0);
        m_blocks.memCopy(pointer, 0, p, 0, m_blockSize);
        m_blocks.free(pointer);
        return ret;
      }
    } else {
      // multi block
      int directPointer = ~pointer;
      int numBlocks = m_blocks.getInt(directPointer, INDEX_NUM_BLOCKS_OFFSET);
      int neededBlocks = 1 + ((size - 1) / m_blockSize); // ceil(a/b)
      if (neededBlocks > numBlocks) {
        int initialSize = maximumCapacityFor(pointer);
        try {
          while (numBlocks++ < neededBlocks) {
            appendBlock(pointer);
          }
        } catch (OutOfMemoryException e) {
          realloc(pointer, initialSize);
          throw e;
        }
      } else if (numBlocks > neededBlocks) {
        while (numBlocks-- > neededBlocks) {
          removeBlock(pointer);
        }

        if (neededBlocks < 2 && pointer < 0) {
          pointer = ~pointer;
        }
      }

      return pointer;
    }
  }

  private void removeBlock(int pointer) {
    assert pointer < 0;

    int rootPtr = ~pointer;

    int numBlocks = m_blocks.getInt(rootPtr, INDEX_NUM_BLOCKS_OFFSET);

    int last_block = numBlocks - 1;
    while (true) {
      int maxCap = maximumCapacityForNumBlocks(numBlocks) / m_blockSize;
      int nextLevelMaxCap = maxCap / m_indexBlockCapacity;
      if (numBlocks - 1 == nextLevelMaxCap) {
        // full tree, need to remove a level
        int removedBlock1 = m_blocks.getInt(rootPtr, INDEX_DATA_OFFSET + 0);
        int removedBlock2 = m_blocks.getInt(rootPtr, INDEX_DATA_OFFSET + 1);
        removedBlock1 = removedBlock1 > 0 ? removedBlock1 : ~removedBlock1;
        m_blocks.memCopy(removedBlock1, 0, rootPtr, 0, m_blockSize);
        free(removedBlock1);
        free(removedBlock2);
        break;
      }

      int blockNum = last_block / nextLevelMaxCap;
      last_block -= blockNum * nextLevelMaxCap;
      if (nextLevelMaxCap > 1) {
        int p = m_blocks.getInt(rootPtr, INDEX_DATA_OFFSET + blockNum);
        if (last_block == 0) {
          m_blocks.setInt(rootPtr, INDEX_NUM_BLOCKS_OFFSET, numBlocks - 1);
          int removedBlock = m_blocks.getInt(rootPtr, INDEX_DATA_OFFSET + blockNum);
          free(removedBlock);
          break;
        } else if (p > 0) {
          // data pointer
          m_blocks.setInt(rootPtr, INDEX_NUM_BLOCKS_OFFSET, numBlocks - 1);
          int removedBlock1 = m_blocks.getInt(rootPtr, INDEX_DATA_OFFSET + blockNum);
          int removedBlock2 = m_blocks.getInt(removedBlock1, INDEX_DATA_OFFSET + 1);
          int preservedBlock = m_blocks.getInt(removedBlock1, INDEX_DATA_OFFSET + 0);
          m_blocks.setInt(rootPtr, INDEX_DATA_OFFSET + 1, preservedBlock);
          free(removedBlock1);
          free(removedBlock2);
          break;
        } else {

          m_blocks.setInt(rootPtr, INDEX_NUM_BLOCKS_OFFSET, numBlocks - 1);
          int nextLevelMemSize = m_blocks.getInt(~p, INDEX_NUM_BLOCKS_OFFSET) * m_blockSize;
          if (nextLevelMemSize - m_blockSize == m_blockSize) {
            // index block referncing two data blocks, removal of one data block
            // will
            // cause removal of index block

            int removedBlock1 = p;
            removedBlock1 = removedBlock1 > 0 ? removedBlock1 : ~removedBlock1;
            int removedBlock2 = m_blocks.getInt(removedBlock1, INDEX_DATA_OFFSET + 1);
            int preservedBlock = m_blocks.getInt(removedBlock1, INDEX_DATA_OFFSET + 0);
            m_blocks.setInt(rootPtr, INDEX_DATA_OFFSET + blockNum, preservedBlock);
            free(removedBlock1);
            free(removedBlock2);
            break;
          } else {
            // keep digging down
            rootPtr = ~p;
          }
        }
      } else {
        m_blocks.setInt(rootPtr, INDEX_NUM_BLOCKS_OFFSET, numBlocks - 1);
        int removedBlock = m_blocks.getInt(rootPtr, INDEX_DATA_OFFSET + blockNum);
        free(removedBlock);
        break;
      }

      numBlocks = m_blocks.getInt(rootPtr, INDEX_NUM_BLOCKS_OFFSET);
    }

  }

  private void appendBlock(int pointer) {
    assert pointer < 0;

    int rootPtr = ~pointer;
    int numBlocks = m_blocks.getInt(rootPtr, INDEX_NUM_BLOCKS_OFFSET);
    int memSize = numBlocks * m_blockSize;

    int newBlock1 = m_blocks.malloc();
    int newBlock2;
    try {
      newBlock2 = m_blocks.malloc();
    } catch (OutOfMemoryException e) {
      free(newBlock1);
      throw e;
    }

    int offset_in_data = memSize;
    while (true) {
      int maxCap = maximumCapacityForNumBlocks(numBlocks);
      if (memSize == maxCap) {
        // full tree, need to add a level
        m_blocks.memCopy(rootPtr, 0, newBlock1, 0, m_blockSize);
        // zero out new root, not strictly needed but nice to have for clarity
        m_blocks.memSet(rootPtr, 0, m_blockSize, 0);
        m_blocks.setInt(rootPtr, INDEX_NUM_BLOCKS_OFFSET, numBlocks + 1);
        m_blocks.setInt(rootPtr, INDEX_DATA_OFFSET + 0, ~newBlock1);
        m_blocks.setInt(rootPtr, INDEX_DATA_OFFSET + 1, newBlock2);
        break;
      }

      int maxCapacityPerIndexPtr = maxCap / m_indexBlockCapacity;
      int blockNum = offset_in_data / maxCapacityPerIndexPtr;
      offset_in_data -= blockNum * maxCapacityPerIndexPtr;
      if (maxCapacityPerIndexPtr > m_blockSize) {
        int p = m_blocks.getInt(rootPtr, INDEX_DATA_OFFSET + blockNum);
        if (offset_in_data == 0) {
          m_blocks.setInt(rootPtr, INDEX_NUM_BLOCKS_OFFSET, numBlocks + 1);
          m_blocks.setInt(rootPtr, INDEX_DATA_OFFSET + blockNum, newBlock1);
          free(newBlock2);
          break;
        } else if (p > 0) {
          // data pointer
          int newIndexBlock = newBlock2;
          m_blocks.setInt(rootPtr, INDEX_NUM_BLOCKS_OFFSET, numBlocks + 1);
          m_blocks.setInt(rootPtr, INDEX_DATA_OFFSET + blockNum, ~newIndexBlock);
          m_blocks.setInt(newIndexBlock, INDEX_NUM_BLOCKS_OFFSET, 2);
          m_blocks.setInt(newIndexBlock, INDEX_DATA_OFFSET + 0, p);
          m_blocks.setInt(newIndexBlock, INDEX_DATA_OFFSET + 1, newBlock1);
          break;
        } else {
          // index pointer, keep digging down
          m_blocks.setInt(rootPtr, INDEX_NUM_BLOCKS_OFFSET, numBlocks + 1);
          rootPtr = ~p;
        }
      } else {
        m_blocks.setInt(rootPtr, INDEX_NUM_BLOCKS_OFFSET, numBlocks + 1);
        m_blocks.setInt(rootPtr, INDEX_DATA_OFFSET + blockNum, newBlock1);
        free(newBlock2);
        break;
      }

      numBlocks = m_blocks.getInt(rootPtr, INDEX_NUM_BLOCKS_OFFSET);
      memSize = numBlocks * m_blockSize;
    }
  }

  // TODO: re-eval this function
  private int findPointerForOffset(int pointer, int offset_in_data) {
    int indexPointer = ~pointer;

    int numBlocks = m_blocks.getInt(indexPointer, INDEX_NUM_BLOCKS_OFFSET);
    int maxCapacityPerIndexPtr = maximumCapacityForNumBlocks(numBlocks) / m_indexBlockCapacity;
    int blockNum = offset_in_data / maxCapacityPerIndexPtr;
    int dataPtr = m_blocks.getInt(indexPointer, INDEX_DATA_OFFSET + blockNum);
    if (maxCapacityPerIndexPtr <= m_blockSize || dataPtr >= 0) {
      return dataPtr;
    } else {
      int new_offset_in_data = offset_in_data - blockNum * maxCapacityPerIndexPtr;
      return findPointerForOffset(dataPtr, new_offset_in_data);
    }
  }

  @Override
  public void free(int pointer) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      int indexPointer = ~pointer;
      int nb = m_blocks.getInt(indexPointer, INDEX_NUM_BLOCKS_OFFSET);
      int memSize = nb * m_blockSize;
      int maxCapacityFor = maximumCapacityForNumBlocks(nb) / m_indexBlockCapacity;
      int numBlocks = 1 + ((memSize - 1) / maxCapacityFor); // ceil(a/b)
      for (int i = 0; i < numBlocks; i++) {
        int p = m_blocks.getInt(indexPointer, i + INDEX_DATA_OFFSET);
        free(p);
      }
      free(indexPointer);
    } else {
      m_blocks.free(pointer);
    }
  }

  @Override
  public short getUpperShort(int pointer, int offset) {
    if (pointer < 0) {
      pointer = getDataBlockPointerFor(pointer, offset);
      offset = retOffset;
    }
    return m_blocks.getUpperShort(pointer, offset);
  }

  @Override
  public short getLowerShort(int pointer, int offset) {
    if (pointer < 0) {
      pointer = getDataBlockPointerFor(pointer, offset);
      offset = retOffset;
    }
    return m_blocks.getLowerShort(pointer, offset);
  }

  @Override
  public void setUpperShort(int pointer, int offset, int s) {
    if (pointer < 0) {
      pointer = getDataBlockPointerFor(pointer, offset);
      offset = retOffset;
    }
    m_blocks.setUpperShort(pointer, offset, s);
  }

  @Override
  public void setLowerShort(int pointer, int offset, int s) {
    if (pointer < 0) {
      pointer = getDataBlockPointerFor(pointer, offset);
      offset = retOffset;
    }
    m_blocks.setLowerShort(pointer, offset, s);
  }

  @Override
  public final void setInt(int pointer, int offset_in_data, int data) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      setIntDataForIndexedBlock(pointer, data, offset_in_data);
    } else {
      m_blocks.setInt(pointer, offset_in_data, data);
    }
  }

  protected void setIntDataForIndexedBlock(int pointer, int data, int offset_in_data) {
    int indexPointer = ~pointer;

    int numBlocks = m_blocks.getInt(indexPointer, INDEX_NUM_BLOCKS_OFFSET);
    int maxCapacityPerIndexPtr = maximumCapacityForNumBlocks(numBlocks) / m_indexBlockCapacity;
    int blockNum = offset_in_data / maxCapacityPerIndexPtr;
    int dataPtr = m_blocks.getInt(indexPointer, INDEX_DATA_OFFSET + blockNum);
    if (maxCapacityPerIndexPtr <= m_blockSize || dataPtr >= 0) {
      int new_offset_in_data = offset_in_data - blockNum * maxCapacityPerIndexPtr;
      m_blocks.setInt(dataPtr, new_offset_in_data, data);
    } else {
      int new_offset_in_data = offset_in_data - blockNum * maxCapacityPerIndexPtr;
      setIntDataForIndexedBlock(dataPtr, data, new_offset_in_data);
    }
  }

  @Override
  public int getInt(int pointer, int offset_in_data) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      return getIntDataForIndexedBlock(pointer, offset_in_data);
    } else {
      return m_blocks.getInt(pointer, offset_in_data);
    }
  }

  public int getIntDataForIndexedBlock(int pointer, int offset_in_data) {
    int indexPointer = ~pointer;

    int numBlocks = m_blocks.getInt(indexPointer, INDEX_NUM_BLOCKS_OFFSET);
    int maxCapacity = maximumCapacityForNumBlocks(numBlocks) / m_indexBlockCapacity;
    int blockNum = offset_in_data / maxCapacity;
    int dataPtr = m_blocks.getInt(indexPointer, INDEX_DATA_OFFSET + blockNum);
    if (maxCapacity <= m_blockSize || dataPtr >= 0) {
      int new_offset_in_data = offset_in_data - blockNum * maxCapacity;
      return m_blocks.getInt(dataPtr, new_offset_in_data);
    } else {
      int new_offset_in_data = offset_in_data - blockNum * maxCapacity;
      return getIntDataForIndexedBlock(dataPtr, new_offset_in_data);
    }
  }

  @Override
  public void setInts(int pointer, int dst_offset_in_record, int src_data[], int src_pos, int length) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      int numBlocks = m_blocks.getInt(~pointer, INDEX_NUM_BLOCKS_OFFSET);
      int currentLevelCapacity = maximumCapacityForNumBlocks(numBlocks);

      assert src_pos >= 0 : "Negative src_pos " + src_pos;
      assert dst_offset_in_record + length <= numBlocks * m_blockSize : String.format(
          "src_pos + length > memSize : %d + %d < %d", src_pos, length, numBlocks * m_blockSize);

      int left_to_copy = length;
      int dst_offset = dst_offset_in_record;
      for (int src_offset = src_pos; src_offset < length;) {
        int this_copy_length;
        if (dst_offset % m_blockSize != 0) {
          this_copy_length = m_blockSize - dst_offset % m_blockSize;
        } else {
          if (left_to_copy > m_blockSize) {
            this_copy_length = m_blockSize;
          } else {
            this_copy_length = left_to_copy;
          }
        }

        int dataPtr = pointer;

        int current_dst_offset = dst_offset;
        int thisLevelCapacity = currentLevelCapacity;
        while (dataPtr < 0) {
          int directDataPtr = ~dataPtr;
          int levelMemSize = m_blocks.getInt(directDataPtr, INDEX_NUM_BLOCKS_OFFSET) * m_blockSize;
          int nextLevelCapacity = thisLevelCapacity / m_indexBlockCapacity;
          while (nextLevelCapacity >= levelMemSize) {
            nextLevelCapacity /= m_indexBlockCapacity;
          }
          int offset_in_index = current_dst_offset / nextLevelCapacity;
          current_dst_offset = current_dst_offset % nextLevelCapacity;
          dataPtr = m_blocks.getInt(directDataPtr, INDEX_DATA_OFFSET + offset_in_index);
          thisLevelCapacity = nextLevelCapacity;
        }

        m_blocks.setInts(dataPtr, current_dst_offset, src_data, src_pos + src_offset,
            this_copy_length);

        src_offset += this_copy_length;
        left_to_copy -= this_copy_length;
        dst_offset += this_copy_length;
      }
    } else {
      m_blocks.setInts(pointer, dst_offset_in_record, src_data, src_pos, length);
    }
  }

  @Override
  public void setChars(int pointer, int dst_int_offset, char[] src_char_data, int src_char_pos, int num_chars) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;

    if (num_chars == 0) {
      return;
    }

    if (pointer < 0) {
      int numBlocks = m_blocks.getInt(~pointer, INDEX_NUM_BLOCKS_OFFSET);
      int currentLevelCapacity = maximumCapacityForNumBlocks(numBlocks);
      int num_ints = 1 + (num_chars - 1) / 2; // ceil (num_chars / 2)

      assert src_char_pos >= 0 : "Negative src_pos " + src_char_pos;
      assert dst_int_offset + num_ints <= numBlocks * m_blockSize : String
          .format("dst_offset_in_record + num_ints > memSize : %d + %d < %d", dst_int_offset, num_ints, numBlocks
              * m_blockSize);

      int chars_left_to_copy = num_chars;
      for (int src_offset_chars = src_char_pos; src_offset_chars < num_chars;) {

        int num_ints_to_copy;
        if (dst_int_offset % m_blockSize != 0) {
          num_ints_to_copy = m_blockSize - dst_int_offset % m_blockSize;
        } else {
          if (chars_left_to_copy > m_blockSize) {
            num_ints_to_copy = m_blockSize;
          } else {
            num_ints_to_copy = chars_left_to_copy;
          }
        }
        int num_chars_to_copy = Math.min(num_ints_to_copy * 2, chars_left_to_copy);

        int dataPtr = pointer;

        int current_dst_offset = dst_int_offset;
        int thisLevelCapacity = currentLevelCapacity;
        while (dataPtr < 0) {
          int directDataPtr = ~dataPtr;
          int levelMemSize = m_blocks.getInt(directDataPtr, INDEX_NUM_BLOCKS_OFFSET) * m_blockSize;
          int nextLevelCapacity = thisLevelCapacity / m_indexBlockCapacity;
          while (nextLevelCapacity >= levelMemSize) {
            nextLevelCapacity /= m_indexBlockCapacity;
          }
          int offset_in_index = current_dst_offset / nextLevelCapacity;
          current_dst_offset = current_dst_offset % nextLevelCapacity;
          dataPtr = m_blocks.getInt(directDataPtr, INDEX_DATA_OFFSET + offset_in_index);
          thisLevelCapacity = nextLevelCapacity;
        }

        m_blocks.setChars(dataPtr, current_dst_offset, src_char_data, src_char_pos + src_offset_chars,
            num_chars_to_copy);

        src_offset_chars += num_chars_to_copy;
        chars_left_to_copy -= num_chars_to_copy;
        dst_int_offset += num_ints_to_copy;
      }
    } else {
      m_blocks.setChars(pointer, dst_int_offset, src_char_data, src_char_pos, num_chars);
    }
  }

  @Override
  public void getChars(int pointer, int src_int_offset, char[] dst_char_data, int dst_char_pos, int num_chars) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;

    if (num_chars == 0) {
      return;
    }

    if (pointer < 0) {
      int numBlocks = m_blocks.getInt(~pointer, INDEX_NUM_BLOCKS_OFFSET);
      int currentLevelCapacity = maximumCapacityForNumBlocks(numBlocks);
      int num_ints = 1 + (num_chars - 1) / 2; // ceil (num_chars / 2)

      assert dst_char_pos >= 0 : "Negative dst_pos " + dst_char_pos;
      assert src_int_offset + num_ints <= numBlocks * m_blockSize : String.format(
          "dst_pos + num_ints > memSize : %d + %d < %d", dst_char_pos, num_ints, numBlocks * m_blockSize);

      int chars_left_to_copy = num_chars;
      for (int dst_offset_chars = dst_char_pos; dst_offset_chars < num_chars;) {
        int num_ints_to_copy;
        if (src_int_offset % m_blockSize != 0) {
          num_ints_to_copy = m_blockSize - src_int_offset % m_blockSize;
        } else {
          if (chars_left_to_copy > m_blockSize) {
            num_ints_to_copy = m_blockSize;
          } else {
            num_ints_to_copy = chars_left_to_copy;
          }
        }
        int num_chars_to_copy = Math.min(num_ints_to_copy * 2, chars_left_to_copy);

        int dataPtr = pointer;

        int current_src_offset = src_int_offset;
        int thisLevelCapacity = currentLevelCapacity;
        while (dataPtr < 0) {
          int directDataPtr = ~dataPtr;
          int levelMemSize = m_blocks.getInt(directDataPtr, INDEX_NUM_BLOCKS_OFFSET) * m_blockSize;
          int nextLevelCapacity = thisLevelCapacity / m_indexBlockCapacity;
          while (nextLevelCapacity >= levelMemSize) {
            nextLevelCapacity /= m_indexBlockCapacity;
          }
          int offset_in_index = current_src_offset / nextLevelCapacity;
          current_src_offset = current_src_offset % nextLevelCapacity;
          dataPtr = m_blocks.getInt(directDataPtr, INDEX_DATA_OFFSET + offset_in_index);
          thisLevelCapacity = nextLevelCapacity;
        }

        m_blocks.getChars(dataPtr, current_src_offset, dst_char_data, dst_char_pos + dst_offset_chars,
            num_chars_to_copy);

        dst_offset_chars += num_chars_to_copy;
        chars_left_to_copy -= num_chars_to_copy;
        src_int_offset += num_ints_to_copy;
      }
    } else {
      m_blocks.getChars(pointer, src_int_offset, dst_char_data, dst_char_pos, num_chars);
    }
  }

  @Override
  public void memSet(int pointer, int src_pos, int length, int value) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      int numBlocks = m_blocks.getInt(~pointer, INDEX_NUM_BLOCKS_OFFSET);
      int currentLevelCapacity = maximumCapacityForNumBlocks(numBlocks);

      assert src_pos >= 0 : "Negative src_pos " + src_pos;
      assert src_pos + length <= numBlocks * m_blockSize : String.format("src_pos + length > memSize : %d + %d < %d",
          src_pos, length, numBlocks * m_blockSize);

      int left_to_copy = length;
      int src1_offset = src_pos;
      while (left_to_copy > 0) {
        int this_set_length;
        if (src1_offset % m_blockSize != 0) {
          this_set_length = m_blockSize - src1_offset % m_blockSize;
        } else {
          if (left_to_copy > m_blockSize) {
            this_set_length = m_blockSize;
          } else {
            this_set_length = left_to_copy;
          }
        }

        int dataPtr = pointer;

        int current_dst_offset = src1_offset;
        int thisLevelCapacity = currentLevelCapacity;
        while (dataPtr < 0) {
          int directDataPtr = ~dataPtr;
          int levelMemSize = m_blocks.getInt(directDataPtr, INDEX_NUM_BLOCKS_OFFSET) * m_blockSize;
          int nextLevelCapacity = thisLevelCapacity / m_indexBlockCapacity;
          while (nextLevelCapacity >= levelMemSize) {
            nextLevelCapacity /= m_indexBlockCapacity;
          }
          int offset_in_index = current_dst_offset / nextLevelCapacity;
          current_dst_offset = current_dst_offset % nextLevelCapacity;
          dataPtr = m_blocks.getInt(directDataPtr, INDEX_DATA_OFFSET + offset_in_index);
          thisLevelCapacity = nextLevelCapacity;
        }

        m_blocks.memSet(dataPtr, current_dst_offset, this_set_length, value);

        left_to_copy -= this_set_length;
        src1_offset += this_set_length;
      }
    } else {
      m_blocks.memSet(pointer, src_pos, length, value);
    }
  }

  @Override
  public void getInts(int pointer, int src_offset_in_record, int dst_data[], int dst_pos, int length) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      int numBlocks = m_blocks.getInt(~pointer, INDEX_NUM_BLOCKS_OFFSET);
      int currentLevelCapacity = maximumCapacityForNumBlocks(numBlocks);

      assert dst_pos >= 0 : "Negative dst_pos " + dst_pos;
      assert src_offset_in_record + length <= numBlocks * m_blockSize : String.format(
          "dst_pos + length > memSize : %d + %d < %d", dst_pos, length, numBlocks * m_blockSize);

      int left_to_copy = length;
      int src_offset = src_offset_in_record;
      for (int dst_offset = dst_pos; dst_offset < length;) {
        int this_copy_length;
        if (src_offset % m_blockSize != 0) {
          this_copy_length = m_blockSize - src_offset % m_blockSize;
        } else {
          if (left_to_copy > m_blockSize) {
            this_copy_length = m_blockSize;
          } else {
            this_copy_length = left_to_copy;
          }
        }

        int dataPtr = pointer;

        int current_src_offset = src_offset;
        int thisLevelCapacity = currentLevelCapacity;
        while (dataPtr < 0) {
          int directDataPtr = ~dataPtr;
          int levelMemSize = m_blocks.getInt(directDataPtr, INDEX_NUM_BLOCKS_OFFSET) * m_blockSize;
          int nextLevelCapacity = thisLevelCapacity / m_indexBlockCapacity;
          while (nextLevelCapacity >= levelMemSize) {
            nextLevelCapacity /= m_indexBlockCapacity;
          }
          int offset_in_index = current_src_offset / nextLevelCapacity;
          current_src_offset = current_src_offset % nextLevelCapacity;
          dataPtr = m_blocks.getInt(directDataPtr, INDEX_DATA_OFFSET + offset_in_index);
          thisLevelCapacity = nextLevelCapacity;
        }

        m_blocks.getInts(dataPtr, current_src_offset, dst_data, dst_pos + dst_offset, this_copy_length);

        dst_offset += this_copy_length;
        left_to_copy -= this_copy_length;
        src_offset += this_copy_length;
      }
    } else {
      m_blocks.getInts(pointer, src_offset_in_record, dst_data, dst_pos, length);
    }
  }

  @Override
  public void getBuffer(int pointer, int src_offset_in_record, IBuffer dst, int length) {
    getInts(pointer, src_offset_in_record, dst.array(), 0, length);
    dst.setUsed(length);
  }

  @Override
  public long getLong(int pointer, int offset_in_data) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      int ilower = getInt(pointer, offset_in_data + 1);
      int iupper = getInt(pointer, offset_in_data);
      long lower = 0x00000000FFFFFFFFL & ilower;
      long upper = ((long) iupper) << 32;
      return upper | lower;
    } else {
      return m_blocks.getLong(pointer, offset_in_data);
    }
  }

  @Override
  public void setLong(int pointer, int offset_in_data, long data) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      // upper int
      int int1 = (int) (data >>> 32);
      // lower int
      int int2 = (int) (data);
      setInt(pointer, offset_in_data, int1);
      setInt(pointer, offset_in_data + 1, int2);
    } else {
      m_blocks.setLong(pointer, offset_in_data, data);
    }
  }

  @Override
  public int computeMemoryUsageFor(int size) {
    return 4 * m_blockSize * calculateNumBlocksFor(size);
  }

  private int calculateNumBlocksFor(int size) {
    return calculateNumBlocksFor(size, m_blockSize);
  }

  public static int calculateNumBlocksFor(int size, int blockSize) {
    if (size <= blockSize) {
      return 1;
    } else {
      int blocks = 1;
      int indexBlockCapacity = blockSize - INDEX_DATA_OFFSET;
      int numDataBlocks = 1 + ((size - 1) / blockSize); // ceil(a/b)
      // capacity of index that supports memory size
      int maxCapacity;
      {
        // ceil(memSize / m_blockSize);
        int neededBlocks = 1 + (size - 1) / blockSize;
        int indexCapacity = blockSize - INDEX_DATA_OFFSET;
        int currIndexCapacity = blockSize;
        while (neededBlocks > 1) {
          currIndexCapacity *= indexCapacity;
          // ceil(neededBlocks / indexCapacity);
          neededBlocks = 1 + (neededBlocks - 1) / indexCapacity;
        }
        maxCapacity = currIndexCapacity;
      }

      if (indexBlockCapacity * blockSize >= size) {
        blocks += numDataBlocks;
      } else {
        // allocating index
        int nextLevelCapacity = maxCapacity / indexBlockCapacity;
        int numIndexBlocksToAllocate = 1 + (size - 1) / nextLevelCapacity;
        int left = size;
        int indexedMemorySize = nextLevelCapacity;
        int blockNum = 0;
        for (; blockNum < numIndexBlocksToAllocate; blockNum++) {
          if (left <= nextLevelCapacity)
            indexedMemorySize = left;
          left -= indexedMemorySize;
          assert left >= 0;

          if (indexedMemorySize < blockSize) {
            blocks += 1;
          } else {
            blocks += calculateNumBlocksFor(indexedMemorySize, blockSize);
          }
        }
      }

      return blocks;
    }
  }

  private int maximumCapacityForNumBlocks(int numBlocks) {
    int currIndexCapacity = m_blockSize;
    while (numBlocks > 1) {
      currIndexCapacity *= m_indexBlockCapacity;
      // ceil(neededBlocks / indexCapacity);
      numBlocks = 1 + (numBlocks - 1) / m_indexBlockCapacity;
    }
    return currIndexCapacity;
  }

  @Override
  public boolean isDebug() {
    return m_blocks.isDebug();
  }

  @Override
  public void setDebug(boolean debug) {
    m_blocks.setDebug(debug);
  }

  @Override
  public void setInitializer(MemInitializer initializer) {
    m_blocks.setInitializer(initializer);
  }

  @Override
  public int usedBlocks() {
    return m_blocks.usedBlocks();
  }

  @Override
  public int blockSize() {
    return m_blocks.blockSize();
  }

  @Override
  public int maxBlocks() {
    return m_blocks.maxBlocks();
  }

  @Override
  public int freeBlocks() {
    return m_blocks.freeBlocks();
  }

  @Override
  public void clear() {
    m_blocks.clear();
  }

  @Override
  public long computeMemoryUsage() {
    return m_blocks.computeMemoryUsage();
  }

  @Override
  public int maximumCapacityFor(int pointer) {
    int capacity = 0;
    if (pointer < 0) {
      capacity = m_blockSize * m_blocks.getInt(~pointer, INDEX_NUM_BLOCKS_OFFSET);
    } else {
      capacity = m_blocks.blockSize();
    }
    return capacity;
  }

  @Override
  public void setGrowthFactor(double d) {
    m_blocks.setGrowthFactor(d);
  }

  @Override
  public double getGrowthFactor() {
    return m_blocks.getGrowthFactor();
  }

  @Override
  public String toString() {
    return m_blocks.toString();
  }

  @Override
  public IBlockAllocator getBlocks() {
    return m_blocks;
  }

  @Override
  public String pointerDebugString(int pointer) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    StringBuilder sb = new StringBuilder();
    if (pointer > 0) {
      sb.append("[");
      for (int i = 0; i < m_blockSize; i++) {
        sb.append(m_blocks.getInt(pointer, i));
        if (i + 1 < m_blockSize)
          sb.append(",");
      }
      sb.append("] ");
    } else {
      Queue<Integer> thisLevel = new LinkedList<Integer>();
      thisLevel.add(pointer);

      Queue<Integer> nextLevel = new LinkedList<Integer>();
      while (!thisLevel.isEmpty()) {
        for (int next : thisLevel) {
          if (next < 0) {
            int indexPointer = ~next;
            int nb = m_blocks.getInt(indexPointer, INDEX_NUM_BLOCKS_OFFSET);
            int memSize = nb * m_blockSize;
            int maxCapacityFor = maximumCapacityForNumBlocks(nb) / m_indexBlockCapacity;
            int numBlocks = 1 + ((memSize - 1) / maxCapacityFor); // ceil(a/b)
            for (int i = 0; i < numBlocks; i++) {
              int child = m_blocks.getInt(indexPointer, i + INDEX_DATA_OFFSET);
              nextLevel.add(child);
            }
            sb.append("[NB=").append(nb).append(", NC=").append(numBlocks).append("]");
          } else {
            sb.append("[");
            for (int i = 0; i < m_blockSize; i++) {
              sb.append(m_blocks.getInt(next, i));
              if (i + 1 < m_blockSize)
                sb.append(",");
            }
            sb.append("] ");
          }
        }
        thisLevel.clear();
        thisLevel.addAll(nextLevel);
        nextLevel.clear();
        sb.append("\n");
      }
    }

    return sb.toString();
  }

  @Override
  public void initialize(int pointer) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      int indexPointer = ~pointer;
      int nb = m_blocks.getInt(indexPointer, INDEX_NUM_BLOCKS_OFFSET);
      int memSize = nb * m_blockSize;
      int maxCapacityFor = maximumCapacityForNumBlocks(nb) / m_indexBlockCapacity;
      int numBlocks = 1 + ((memSize - 1) / maxCapacityFor); // ceil(a/b)
      for (int i = 0; i < numBlocks; i++) {
        int p = m_blocks.getInt(indexPointer, i + INDEX_DATA_OFFSET);
        initialize(p);
      }
    } else {
      m_blocks.initialize(pointer);
    }
  }

  // this is super ugly, but since nothing here is thread safe anyway it's okay.
  int retOffset;
  // TODO: can this be iterative? can we eliminate the call to
  // maximumCapacityForNumBlocks?
  protected int getDataBlockPointerFor(int pointer, int offset) {
    assert pointer < 0;
    int indexPointer = ~pointer;

    int numBlocks = m_blocks.getInt(indexPointer, INDEX_NUM_BLOCKS_OFFSET);
    int maxCapacityPerIndexPtr = maximumCapacityForNumBlocks(numBlocks) / m_indexBlockCapacity;
    int blockNum = offset / maxCapacityPerIndexPtr;
    int dataPtr = m_blocks.getInt(indexPointer, INDEX_DATA_OFFSET + blockNum);
    if (maxCapacityPerIndexPtr <= m_blockSize || dataPtr >= 0) {
      int new_offset_in_data = offset - blockNum * maxCapacityPerIndexPtr;
      retOffset = new_offset_in_data;
      return dataPtr;
    } else {
      int new_offset_in_data = offset - blockNum * maxCapacityPerIndexPtr;
      return getDataBlockPointerFor(dataPtr, new_offset_in_data);
    }
  }

  @Override
  public float getFloat(int pointer, int offset) {
    return Float.intBitsToFloat(getInt(pointer, offset));
  }

  @Override
  public void setFloat(int pointer, int offset, float f) {
    setInt(pointer, offset, Float.floatToIntBits(f));
  }

  @Override
  public double getDouble(int pointer, int offset_in_data) {
    return Double.longBitsToDouble(getLong(pointer, offset_in_data));
  }

  @Override
  public void setDouble(int pointer, int offset_in_data, double data) {
    setLong(pointer, offset_in_data, Double.doubleToLongBits(data));
  }
}
