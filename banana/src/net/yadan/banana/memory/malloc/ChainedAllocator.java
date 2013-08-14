/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory.malloc;

import net.yadan.banana.memory.IBlockAllocator;
import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.MemInitializer;
import net.yadan.banana.memory.OutOfBoundsAccess;
import net.yadan.banana.memory.OutOfMemoryException;
import net.yadan.banana.memory.block.BlockAllocator;


public class ChainedAllocator implements IMemAllocator {

  private static final int NEXT_OFFSET = 0;
  private static final int DATA_OFFSET = 1;

  private IBlockAllocator m_blocks;
  private int m_blockSize;

  public ChainedAllocator(int maxBlocks, int blockSize) {
    this(maxBlocks, blockSize, null);
  }

  public ChainedAllocator(int maxBlocks, int blockSize, MemInitializer initializer) {
    this(maxBlocks, blockSize, 0, initializer);
  }

  public ChainedAllocator(int maxBlocks, int blockSize, double growthFactor) {
    this(maxBlocks, blockSize, growthFactor, null);
  }

  public ChainedAllocator(int maxBlocks, int blockSize, double growthFactor,
      MemInitializer initializer) {

    if (blockSize < 2) {
      throw new IllegalArgumentException("Minimum block size is 2");
    }
    m_blocks = new BlockAllocator(maxBlocks, blockSize, growthFactor, initializer);
    m_blockSize = blockSize;
  }

  public ChainedAllocator(IBlockAllocator blocks) {
    m_blocks = blocks;
    m_blockSize = blocks.blockSize();
  }

  @Override
  public int malloc(int size) throws OutOfMemoryException {
    if (size < 0) {
      throw new IllegalArgumentException("malloc size must be non-negative");
    }

    if (size <= m_blockSize) {
      return m_blocks.malloc();
    } else {
      return ~multiBlockMalloc(size);
    }
  }

  private int multiBlockMalloc(int size) {
    int dataSize = m_blockSize - DATA_OFFSET;
    int remains = size - dataSize;
    int head = m_blocks.malloc();
    m_blocks.setInt(head, NEXT_OFFSET, -1);

    int link = head;
    try {
      while (remains > 0) {
        int next = m_blocks.malloc();
        m_blocks.setInt(next, NEXT_OFFSET, -1);
        m_blocks.setInt(link, NEXT_OFFSET, next);
        link = next;
        remains -= dataSize;
      }
    } catch (OutOfMemoryException e) {
      free(~head);
      throw e;
    }

    return head;
  }


  @Override
  public int realloc(int pointer, int newSize) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (newSize < 0) {
      throw new IllegalArgumentException("malloc size must be non-negative");
    }

    if (pointer < 0) {
      if (newSize <= m_blockSize) {
        int np = m_blocks.malloc();
        m_blocks.memCopy(~pointer, DATA_OFFSET, np , 0, m_blockSize - DATA_OFFSET);
        int next = m_blocks.getInt(~pointer, NEXT_OFFSET);
        if (next != -1) {
          m_blocks.memCopy(next, DATA_OFFSET, np, m_blockSize - DATA_OFFSET, DATA_OFFSET);
        }
        free(pointer);
        return np;
      } else {
        int next = ~pointer;
        int last = -1;
        int blocksRemaining = 1 + ((newSize - 1) / (m_blockSize - DATA_OFFSET)); // ceil(a/b)
        do {
          blocksRemaining--;
          last = next;
          next = m_blocks.getInt(next, NEXT_OFFSET);
        } while (next != -1 && blocksRemaining != 0);

        if (blocksRemaining > 0) {
          int link = last;
          try {
            while(blocksRemaining > 0) {
              next = m_blocks.malloc();
              m_blocks.setInt(next, NEXT_OFFSET, -1);
              m_blocks.setInt(link, NEXT_OFFSET, next);
              link = next;
              blocksRemaining--;
            }
          } catch (OutOfMemoryException e) {
            // TODO : is it possible to implement this handling with a realloc to shrink back?
            int before_last = last;
            last = m_blocks.getInt(last, NEXT_OFFSET);
            while(last != -1) {
              int f = m_blocks.getInt(last, NEXT_OFFSET);
              m_blocks.free(last);
              last = f;
            }
            m_blocks.setInt(before_last, NEXT_OFFSET, -1);
            throw e;
          }
        } else {
          // free remains if any
          int link = next;
          while (link != -1) {
            int p = link;
            link = m_blocks.getInt(link, NEXT_OFFSET);
            free(p);
          }
          m_blocks.setInt(last, NEXT_OFFSET, -1);
        }
        return pointer;
      }
    } else {
      // current memory is a single block
      if (newSize <= m_blockSize) {
        // nothing to do
        return pointer;
      } else { // new size > block size
        // allocate new multiblock, copy data
        int p = malloc(newSize);
        assert p < 0; // p must be multiblock
        int dst = ~p;
        m_blocks.memCopy(pointer, 0, dst, DATA_OFFSET, m_blockSize - DATA_OFFSET);
        dst = m_blocks.getInt(dst, NEXT_OFFSET);
        m_blocks.memCopy(pointer, m_blockSize - DATA_OFFSET, dst, DATA_OFFSET, DATA_OFFSET);
        free(pointer);
        return p;
      }
    }
  }

  @Override
  public void free(int pointer) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      int directPointer = ~pointer;
      int next;
      do {
        next = m_blocks.getInt(directPointer, NEXT_OFFSET);
        m_blocks.free(directPointer);
        directPointer = next;
      } while (next != -1);

    } else {
      m_blocks.free(pointer);
    }
  }

  @Override
  public final void setInt(int pointer, int offset_in_data, int data) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      setIntDataMultiBlock(pointer, data, offset_in_data);
    } else {
      m_blocks.setInt(pointer, offset_in_data, data);
    }
  }

  protected void setIntDataMultiBlock(int pointer, int data, int offset_in_data) {
    int dataSize = m_blockSize - DATA_OFFSET;
    int current = ~pointer;
    while (offset_in_data >= dataSize) {
      current = m_blocks.getInt(current, NEXT_OFFSET);
      if (current == -1) {
        throw new OutOfBoundsAccess("Accessing pointer beyond allocation size");
      }
      offset_in_data -= dataSize;
    }
    m_blocks.setInt(current, DATA_OFFSET + offset_in_data, data);
  }

  @Override
  public int getInt(int pointer, int offset_in_data) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      return getIntDataMultiBlock(pointer, offset_in_data);
    } else {
      return m_blocks.getInt(pointer, offset_in_data);
    }
  }

  public int getIntDataMultiBlock(int pointer, int offset_in_data) {
    int dataSize = m_blockSize - DATA_OFFSET;
    int current = ~pointer;
    while (offset_in_data >= dataSize) {
      current = m_blocks.getInt(current, NEXT_OFFSET);
      if (current == -1) {
        throw new OutOfBoundsAccess("Accessing pointer beyond allocation size");
      }
      offset_in_data -= dataSize;
    }

    return m_blocks.getInt(current, DATA_OFFSET + offset_in_data);
  }

  @Override
  public void setInts(int pointer, int dst_offset_in_record,
      int src_data[], int src_pos, int length) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {

      int dataSizePerBlock = m_blockSize - DATA_OFFSET;
      int current = ~pointer;

      while (dst_offset_in_record > dataSizePerBlock) {
        dst_offset_in_record -= dataSizePerBlock;
        current = m_blocks.getInt(current, NEXT_OFFSET);
      }

      while (length > 0) {
        if (current == -1) {
          throw new OutOfBoundsAccess("Accessing pointer beyond allocation size");
        }

        int copy_length;
        int dst_offset = 0;
        if (length > dataSizePerBlock - dst_offset_in_record) {
          copy_length = dataSizePerBlock - dst_offset_in_record;
          dst_offset = dst_offset_in_record;
          dst_offset_in_record = 0;
        } else {
          copy_length = length - dst_offset_in_record;
          dst_offset = dst_offset_in_record;
        }

        m_blocks.setInts(current, DATA_OFFSET + dst_offset, src_data, src_pos, copy_length);
        length -= copy_length;
        src_pos += copy_length;
        current = m_blocks.getInt(current, NEXT_OFFSET);
      }
    } else {
      m_blocks.setInts(pointer, dst_offset_in_record, src_data, src_pos, length);
    }
  }

  @Override
  public void getInts(int pointer, int src_offset_in_record,
      int dst_data[], int dst_pos, int length) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      int dataSize = m_blockSize - DATA_OFFSET;
      int current = ~pointer;

      while (src_offset_in_record >= dataSize) {
        src_offset_in_record -= dataSize;
        current = m_blocks.getInt(current, NEXT_OFFSET);
      }

      while (length > 0) {
        if (current == -1) {
          throw new OutOfBoundsAccess("Accessing pointer beyond allocation size");
        }

        int copy_length;
        int src_offset = 0;
        if (length > dataSize - src_offset_in_record) {
          copy_length = dataSize - src_offset_in_record;
          src_offset = src_offset_in_record;
          src_offset_in_record = 0;
        } else {
          copy_length = length - src_offset_in_record;
          src_offset = src_offset_in_record;
        }

        m_blocks.getInts(current, DATA_OFFSET + src_offset, dst_data, dst_pos, copy_length);
        length -= copy_length;
        dst_pos += copy_length;
        current = m_blocks.getInt(current, NEXT_OFFSET);
      }
    } else {
      m_blocks.getInts(pointer, src_offset_in_record, dst_data, dst_pos, length);
    }
  }


  @Override
  public void memSet(int pointer, int srcPos, int length, int value) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      int dataSize = m_blockSize - DATA_OFFSET;
      int current = ~pointer;

      while (srcPos >= dataSize) {
        srcPos -= dataSize;
        current = m_blocks.getInt(current, NEXT_OFFSET);
      }

      while (length > 0) {
        if (current == -1) {
          throw new OutOfBoundsAccess("Accessing pointer beyond allocation size");
        }

        int set_length;
        int src_offset = 0;
        if (length > dataSize - srcPos) {
          set_length = dataSize - srcPos;
          src_offset = srcPos;
          srcPos = 0;
        } else {
          set_length = length - srcPos;
          src_offset = srcPos;
        }

        m_blocks.memSet(current, DATA_OFFSET + src_offset, set_length, value);
        length -= set_length;
        current = m_blocks.getInt(current, NEXT_OFFSET);
      }
    } else {
      m_blocks.memSet(pointer, srcPos, length, value);
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
    assert pointer != -1 : "Invalid pointer " + pointer;;
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
    if (size <= m_blockSize) {
      return 4 * m_blockSize;
    } else {
      return 4 * m_blockSize * (1 + (size - 1) / (m_blockSize - DATA_OFFSET));
    }
  }

  @Override
  public int blockSize() {
    return m_blocks.blockSize();
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
  public String toString() {
    return m_blocks.toString();
  }

  @Override
  public int maximumCapacityFor(int pointer) {
    int capacity = 0;
    if (pointer < 0) {
      int directPointer = ~pointer;
      int next;
      do {
        next = m_blocks.getInt(directPointer, NEXT_OFFSET);
        capacity += (m_blockSize - DATA_OFFSET);
        directPointer = next;
      } while (next != -1);

    } else {
      capacity = m_blockSize;
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
  public IBlockAllocator getBlocks() {
    return m_blocks;
  }

  @Override
  public String pointerDebugString(int pointer) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;;
    StringBuilder sb = new StringBuilder();
    if (pointer < 0) {
      int directPointer = ~pointer;
      int next;
      do {
        next = m_blocks.getInt(directPointer, NEXT_OFFSET);
        sb.append("[");
        for (int i = 1; i < m_blockSize; i++) {
          sb.append(getInt(directPointer, i));
          if (i + 1 < m_blockSize) {
            sb.append(",");
          }
        }
        sb.append("]");
        if (next != -1) {
          sb.append("->");
        }
        directPointer = next;
      } while (next != -1);

    } else {
      sb.append("[");
      for (int i = 0; i < m_blockSize; i++) {
        sb.append(getInt(pointer, i));
        if (i + 1 < m_blockSize) {
          sb.append(",");
        }
      }
      sb.append("]");

    }

    return sb.toString();
  }

  @Override
  public void initialize(int pointer) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      int directPointer = ~pointer;
      int next;
      do {
        next = m_blocks.getInt(directPointer, NEXT_OFFSET);
        m_blocks.initialize(directPointer);
        m_blocks.setInt(directPointer, NEXT_OFFSET, next);
        directPointer = next;
      } while (next != -1);

    } else {
      m_blocks.initialize(pointer);
    }
  }

  @Override
  public short getUpperShort(int pointer, int offset) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      pointer = getDataBlockPointerFor(pointer, offset);
      offset = retOffset;
    }
    return m_blocks.getUpperShort(pointer, offset);
  }

  @Override
  public short getLowerShort(int pointer, int offset) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      pointer = getDataBlockPointerFor(pointer, offset);
      offset = retOffset;
    }
    return m_blocks.getLowerShort(pointer, offset);
  }

  @Override
  public void setUpperShort(int pointer, int offset, int s) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      pointer = getDataBlockPointerFor(pointer, offset);
      offset = retOffset;
    }
    m_blocks.setUpperShort(pointer, offset, s);
  }

  @Override
  public void setLowerShort(int pointer, int offset, int s) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      pointer = getDataBlockPointerFor(pointer, offset);
      offset = retOffset;
    }
    m_blocks.setLowerShort(pointer, offset, s);
  }

  // this is super ugly, but since nothing here is thread safe anyway it's okay.
  int retOffset;
  protected int getDataBlockPointerFor(int pointer, int offset) {
    assert pointer < 0;

    int dataSize = m_blockSize - DATA_OFFSET;
    int current = ~pointer;
    while (offset >= dataSize) {
      current = m_blocks.getInt(current, NEXT_OFFSET);
      if (current == -1) {
        throw new OutOfBoundsAccess("Accessing pointer beyond allocation size");
      }
      offset -= dataSize;
    }
    retOffset = DATA_OFFSET + offset;
    return current;
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

  @Override
  public void setChars(int pointer, int dst_offset_in_record, char[] src_data, int src_pos, int num_chars) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {

      int dataSizePerBlock = m_blockSize - DATA_OFFSET;
      int current = ~pointer;

      while (dst_offset_in_record > dataSizePerBlock) {
        dst_offset_in_record -= dataSizePerBlock;
        current = m_blocks.getInt(current, NEXT_OFFSET);
      }

      int num_ints = 1 + (num_chars - 1) / 2;
      int chars_remaining = num_chars;
      while (num_ints > 0) {
        if (current == -1) {
          throw new OutOfBoundsAccess("Accessing pointer beyond allocation size");
        }

        int copy_length;
        int num_chars_to_copy;
        int dst_offset = 0;
        if (num_ints > dataSizePerBlock - dst_offset_in_record) {
          copy_length = dataSizePerBlock - dst_offset_in_record;
          dst_offset = dst_offset_in_record;
          dst_offset_in_record = 0;
        } else {
          copy_length = num_ints - dst_offset_in_record;
          dst_offset = dst_offset_in_record;
        }
        num_chars_to_copy = copy_length * 2;
        if (num_chars_to_copy > chars_remaining) {
          num_chars_to_copy = chars_remaining;
        }

        m_blocks.setChars(current, DATA_OFFSET + dst_offset, src_data, src_pos * 2, num_chars_to_copy);
        num_ints -= copy_length;
        src_pos += copy_length;
        chars_remaining -= num_chars_to_copy;
        current = m_blocks.getInt(current, NEXT_OFFSET);
      }
    } else {
      m_blocks.setChars(pointer, dst_offset_in_record, src_data, src_pos, num_chars);
    }
  }

  @Override
  public void getChars(int pointer, int src_offset_in_record, char[] dst_data, int dst_pos, int num_chars) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    if (pointer < 0) {
      int dataSize = m_blockSize - DATA_OFFSET;
      int current = ~pointer;

      while (src_offset_in_record >= dataSize) {
        src_offset_in_record -= dataSize;
        current = m_blocks.getInt(current, NEXT_OFFSET);
      }

      int num_ints = 1 + (num_chars - 1) / 2;
      int chars_remaining = num_chars;
      while (num_ints > 0) {
        if (current == -1) {
          throw new OutOfBoundsAccess("Accessing pointer beyond allocation size");
        }

        int copy_length;
        int num_chars_to_copy;
        int src_offset = 0;
        if (num_ints > dataSize - src_offset_in_record) {
          copy_length = dataSize - src_offset_in_record;
          src_offset = src_offset_in_record;
          src_offset_in_record = 0;
        } else {
          copy_length = num_ints - src_offset_in_record;
          src_offset = src_offset_in_record;
        }

        num_chars_to_copy = copy_length * 2;
        if (num_chars_to_copy > chars_remaining) {
          num_chars_to_copy = chars_remaining;
        }

        m_blocks.getChars(current, DATA_OFFSET + src_offset, dst_data, dst_pos * 2, num_chars_to_copy);
        num_ints -= copy_length;
        dst_pos += copy_length;
        chars_remaining -= num_chars_to_copy;
        current = m_blocks.getInt(current, NEXT_OFFSET);
      }
    } else {
      m_blocks.getChars(pointer, src_offset_in_record, dst_data, dst_pos, num_chars);
    }
  }
}
