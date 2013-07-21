/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory.block;

import net.yadan.banana.memory.IBlockAllocator;
import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.MemInitializer;
import net.yadan.banana.memory.OutOfMemoryException;
import net.yadan.banana.memory.initializers.PrototypeInitializer;


/**
 * A fixed size block allocator. total memory is limited to a single int array
 * which contain at most about 2B ints (Integer.MAX_VALUE)
 *
 * Upon construction, the block size and the number of blocks are specified. The
 * maximum block number is Integer.MAX_VALUE / block_size.
 *
 * If you require access to more blocks than this, use {@link BigBlockAllocator}
 * - which can access up to 2B blocks, regardless of the block size.
 *
 * @author omry
 * created 21/4/2013
 */
public class BlockAllocator implements IBlockAllocator {

  protected final int m_blockSize;

  private int m_watermark;
  private int m_free;
  private int m_head;

  int m_buffer[];

  private int m_maxCapacity;

  private MemInitializer m_initializer;

  private boolean m_debug;

  private double m_growthFactor;

  private int m_reservedBlocks;


  /**
   * @param maxBlocks number of blocks to reserve space for
   * @param blockSize record size in ints.
   */
  public BlockAllocator(int maxBlocks, int blockSize) {
    this(maxBlocks, blockSize, null);
  }

  /**
   * @param maxBlocks number of records to reserve space for
   * @param blockSize record size in ints.
   * @param initializer a callback to initialize newly allocated records
   */
  public BlockAllocator(int maxBlocks, int blockSize, MemInitializer initializer) {
    this(maxBlocks, blockSize, 0, initializer);
  }

  /**
   * @param maxBlocks number of records to reserve space for
   * @param blockSize record size in ints.
   * @param growthFactor determines by how much to grow buffer when it runs out
   *          of memory. 0 to disable growth
   */
  public BlockAllocator(int maxBlocks, int blockSize, double growthFactor) {
    this(maxBlocks, blockSize, growthFactor, null);
  }

  /**
   * @param maxBlocks number of records to reserve space for
   * @param blockSize record size in ints.
   * @param growthFactor determines by how much to grow buffer when it runs out
   *          of memory. 0 to disable growth
   * @param initializer a callback to initialize newly allocated records
   */
  public BlockAllocator(int maxBlocks, int blockSize, double growthFactor,
      MemInitializer initializer) {
    m_reservedBlocks = 1;
    m_head = -1;
    m_blockSize = blockSize;
    m_maxCapacity = maxBlocks + m_reservedBlocks;
    m_growthFactor = growthFactor;
    if (initializer == null) {
      initializer = new PrototypeInitializer(blockSize);
    }
    m_debug = false;
    m_initializer = initializer;

    if (maxBlocks < 1)
      throw new IllegalArgumentException("maxBlocks " + maxBlocks + " < 1");

    // block 0 is reserved
    long size = (m_reservedBlocks + (long) maxBlocks) * m_blockSize;
    if (size > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Attempted to allocate " + size
          + " ints, which is greated than Integer.MAX_VALUE (" + Integer.MAX_VALUE + ")");
    }

    m_buffer = new int[(int) size];
    clear();
  }

  /**
   * Allocates a single block and returns a pointer to that block
   *
   * @return pointer to newly allocated block
   *
   * @throws OutOfMemoryException : if there are 0 free blocks
   */
  @Override
  public int malloc() throws OutOfMemoryException {
    if (m_head == -1) {
      if (m_watermark == m_maxCapacity) {
        if (m_growthFactor == 0) {
          throw new OutOfMemoryException("Out of memory (" + maxBlocks() + "/" + usedBlocks()
              + " blocks used)");
        } else {
          increaseSize();
          return malloc();
        }
      } else {
        m_head = m_watermark;
        set_next(m_head, -1);
        m_watermark++;
      }
    } else {
      m_free--;
    }
    int oldHead = m_head;
    m_head = next(oldHead);
    set_next(oldHead, -1);
    m_initializer.initialize(this, oldHead, m_blockSize);
    return oldHead;
  }

  private void increaseSize() {
    int newMaxCapacity = m_reservedBlocks + Math.max(maxBlocks() + 1, (int) (maxBlocks() * m_growthFactor));
    int new_buffer[] = new int[newMaxCapacity * blockSize()];
    System.arraycopy(m_buffer, 0, new_buffer, 0, m_buffer.length);
    m_maxCapacity = newMaxCapacity;
    m_buffer = new_buffer;
  }

  @Override
  public void free(int pointer) {
    assert pointer != 0 : "pointer 0 should not be freed";
    assert pointer != -1 : "pointer -1 should not be freed";
    set_next(pointer, m_head);
    m_head = pointer;
    m_free++;
  }

  @Override
  public void memCopy(int srcPtr, int srcPos, int dstPtr, int dstPos, int length) {
    assert srcPtr >= 0 : "Negative pointer : " + srcPtr;
    assert dstPtr >= 0 : "Negative pointer : " + srcPtr;
    assert length <= m_blockSize : "length > m_blockSize";
    assert srcPos + length <= m_blockSize : "src overflow";
    assert dstPos + length <= m_blockSize : "dst overflow";
    System.arraycopy(m_buffer, srcPtr * m_blockSize + srcPos, m_buffer, dstPtr * m_blockSize + dstPos, length);
  }

  @Override
  public void memSet(int pointer, int srcPos, int length, int value) {
    assert pointer >= 0 : "Negative pointer : " + pointer;
    assert length <= m_blockSize : "length > m_blockSize";
    assert srcPos + length <= m_blockSize : "overflow";

    int p = pointer * m_blockSize;
    for (int i = srcPos; i < srcPos + length; i++) {
      m_buffer[p + i] = value;
    }
  }

  @Override
  public short getUpperShort(int pointer, int offset) {
    assert pointer >= 0 : "Negative pointer : " + pointer;
    assert offset >= 0 : "Negative offset_in_data " + offset;
    assert offset < m_blockSize : String.format("offset >= m_blockSize : %d >= %d", offset, m_blockSize);
    return (short) (m_buffer[pointer * m_blockSize + offset] >>> 16);

  }

  @Override
  public short getLowerShort(int pointer, int offset) {
    assert pointer >= 0 : "Negative pointer : " + pointer;
    assert offset >= 0 : "Negative offset_in_data " + offset;
    assert offset < m_blockSize : String.format("offset >= m_blockSize : %d >= %d", offset, m_blockSize);
    return (short) (m_buffer[pointer * m_blockSize + offset]);
  }

  @Override
  public void setUpperShort(int pointer, int offset, int s) {
    assert pointer >= 0 : "Negative pointer : " + pointer;
    assert offset >= 0 : "Negative offset " + offset;
    assert offset < m_blockSize : String.format("offset >= m_blockSize : %d >= %d", offset, m_blockSize);

    int off = pointer * m_blockSize + offset;
    int lower = m_buffer[off] & 0x0000ffff;
    m_buffer[off] = (s << 16) | lower;
  }

  @Override
  public void setLowerShort(int pointer, int offset, int s) {
    assert pointer >= 0 : "Negative pointer : " + pointer;
    assert offset >= 0 : "Negative offset " + offset;
    assert offset < m_blockSize : String.format("offset >= m_blockSize : %d >= %d", offset, m_blockSize);

    int off = pointer * m_blockSize + offset;
    int upper = m_buffer[off] & 0xffff0000;
    m_buffer[off] = upper | (s & 0x0000ffff);
  }

  @Override
  public int getInt(int pointer, int offset_in_data) {
    assert pointer >= 0 : "Negative pointer : " + pointer;
    assert offset_in_data >= 0 : "Negative offset_in_data " + offset_in_data;
    assert offset_in_data < m_blockSize : String.format("offset_in_data >= m_blockSize : %d >= %d",
        offset_in_data, m_blockSize);
    return m_buffer[pointer * m_blockSize + offset_in_data];
  }

  @Override
  public void setInt(int pointer, int offset_in_data, int data) {
    assert pointer >= 0 : "Negative pointer : " + pointer;
    assert offset_in_data >= 0 : "Negative offset_in_data " + offset_in_data;
    assert offset_in_data < m_blockSize : String.format("offset_in_data >= m_blockSize : %d >= %d",
        offset_in_data, m_blockSize);
    m_buffer[pointer * m_blockSize + offset_in_data] = data;
  }

  @Override
  public void setInts(int pointer, int dst_offset_in_record,
      int src_data[], int src_pos, int length) {
    assert pointer >= 0 : "Negative pointer : " + pointer;
    assert src_pos >= 0 : "Negative src_pos";
    assert src_pos + length <= src_data.length : String.format(
        "src_pos + length > src_data.length : %d + %d > %d", src_pos, length, src_data.length);
    assert pointer * m_blockSize + length <= m_buffer.length : String.format(
        "pointer * blockSize + length > m_buffer.length  : %d + %d >= %d", pointer * m_blockSize,
        length, m_buffer.length);
    assert dst_offset_in_record + length <= m_blockSize : String.format(
        "dst_offset_in_record + length > m_blockSize   : %d + %d >= %d", dst_offset_in_record,
        length, m_blockSize);
    System.arraycopy(src_data, src_pos, m_buffer, pointer * m_blockSize + dst_offset_in_record,
        length);
  }
  
  @Override
  public void getInts(int pointer, int src_offset_in_record,
      int dst_data[], int dst_pos, int length) {

    assert pointer >= 0 : "Negative pointer : " + pointer;
    assert pointer * m_blockSize < m_buffer.length : String.format(
        "pointer >= m_buffer.length : %d < 0", pointer, m_buffer.length);
    assert src_offset_in_record >= 0 : String.format("src_offset_in_record < 0 : %d < 0",
        src_offset_in_record);
    assert src_offset_in_record < m_blockSize : String.format(
        "src_offset_in_record >= m_blockSize : %d >= %d", src_offset_in_record, m_blockSize);
    assert dst_pos >= 0 : String.format("dst_pos < 0 : %d", dst_pos);
    assert dst_pos + length <= dst_data.length : String.format(
        "dst_pos + length > dst_data.length : %d + %d >= %d", dst_pos, length, dst_data.length);

    System.arraycopy(m_buffer, pointer * m_blockSize + src_offset_in_record, dst_data, dst_pos,
        length);
  }
  
  @Override
  public void setChars(int pointer, int dst_offset, char[] src_data, int src_pos, int num_chars) {
    if (num_chars == 0) {
      return;
    }
    int numInts = 1 + (num_chars - 1) / 2; // ceil(num_chars/2)
    for (int i = dst_offset, src_index = src_pos, num_copied = 0; i < dst_offset + numInts; i++, src_index += 2) {
      char c1 = src_data[src_index];
      setUpperShort(pointer, i, c1);
      num_copied++;
      if (num_copied < num_chars) {
        char c2 = src_data[src_index + 1];
        setLowerShort(pointer, i, c2);
        num_copied++;
      }
    }
  }
  
  @Override
  public void getChars(int pointer, int src_offset, char[] dst_data, int dst_pos, int num_chars) {
    if (num_chars == 0) {
      return;
    }

    int numInts = 1 + (num_chars - 1) / 2; // ceil(length/2)
    for (int i = src_offset, dst_index = dst_pos, num_copied = 0; i < src_offset + numInts; i++, dst_index += 2) {
      dst_data[dst_index] = (char) getUpperShort(pointer, i);
      num_copied++;
      if (num_copied < num_chars) {
        dst_data[dst_index + 1] = (char) getLowerShort(pointer, i);
        num_copied++;
      }
    }
  }

  @Override
  public void getBuffer(int pointer, int src_offset_in_record, IBuffer dst, int num_chars) {
    getInts(pointer, src_offset_in_record, dst.array(), 0, num_chars);
    dst.setUsed(num_chars);
  }

  @Override
  public long getLong(int pointer, int offset_in_data) {
    int ilower = getInt(pointer, offset_in_data + 1);
    int iupper = getInt(pointer, offset_in_data);
    long lower = 0x00000000FFFFFFFFL & ilower;
    long upper = ((long) iupper) << 32;
    long ret = upper | lower;
    return ret;
  }

  @Override
  public void setLong(int pointer, int offset_in_data, long data) {
    // upper int
    setInt(pointer, offset_in_data, (int) (data >> 32));
    // lower int
    setInt(pointer, offset_in_data + 1, (int) (data));
  }

  /**
   * @return the number of free blocks
   */
  @Override
  public int freeBlocks() {
    return m_free + m_maxCapacity - m_watermark;
  }

  /**
   * @return the total block capacity for this allocator
   */
  @Override
  public int maxBlocks() {
    return m_maxCapacity - m_reservedBlocks; // block 0 is reserved
  }

  /**
   * @return number of used blocks
   */
  @Override
  public int usedBlocks() {
    return maxBlocks() - freeBlocks();
  }

  /**
   * @return the fixed block size for this allocator
   */
  @Override
  public int blockSize() {
    return m_blockSize;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    try {
      s.append(String.format("IntAllocator %s/%s records of %d ints used, total ints allocated %d",
          maxBlocks() - freeBlocks(), maxBlocks(), m_blockSize, m_buffer.length));
      if (m_debug) {
        s.append('\n');
        for (int i = m_reservedBlocks; i < m_maxCapacity; i++) {
          s.append('(');
          for (int j = 0; j < m_blockSize; j++) {
            s.append(getInt(i, j));
            if (j != m_blockSize - 1) {
              s.append(',');
            }
          }
          s.append(')');
          if (i != m_maxCapacity - 1) {
            s.append(',');
          }
        }
      }
    } catch (RuntimeException e) {
      s.append(" :: Exception inToString() " + e.getClass().getName() + " : " + e.getMessage());
    }
    return s.toString();
  }

  private int next(int node) {
    return m_buffer[node * m_blockSize];
  }

  private void set_next(int node, int next) {
    m_buffer[node * m_blockSize] = next;
  }

  @Override
  public void clear() {
    m_head = -1;
    m_watermark = m_reservedBlocks;
    m_free = 0;
    set_next(1, -1);
  }

  public String debugString() {
    StringBuilder sb = new StringBuilder();
    for (int d : m_buffer) {
      sb.append(String.format("%05X", d & 0xFFFFF)).append(' ');
    }
    return sb.toString();
  }

  public static int getIntArraySize(int capacity, int blockSize) {
    return capacity * blockSize;
  }

  /**
   * Sets the allocator growth factor.
   *
   * @param d new growth factor, 0 to disable growth (default)
   */
  @Override
  public void setGrowthFactor(double d) {
    m_growthFactor = d;
  }

  /**
   * @return the current list growth factor
   */
  @Override
  public double getGrowthFactor() {
    return m_growthFactor;
  }

  @Override
  public boolean isDebug() {
    return m_debug;
  }

  @Override
  public void setDebug(boolean debug) {
    m_debug = debug;
  }

  public MemInitializer getInitializer() {
    return m_initializer;
  }

  @Override
  public void setInitializer(MemInitializer initializer) {
    m_initializer = initializer;
  }

  @Override
  public void initialize(int pointer) {
    m_initializer.initialize(this, pointer, m_blockSize);
  }

  @Override
  public long computeMemoryUsage() {
    return 4 * (long)m_buffer.length;
  }

  @Override
  public int maximumCapacityFor(int pointer) {
    return m_blockSize;
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
