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
 * @author omry
 * created 20/5/2013
 */
public class BigBlockAllocator implements IBlockAllocator {

  private static final int MAX_INTS_PER_ARRAY;
  static {
    String max = System.getProperty("BigIntBlockAllocator.MAX_INTS_PER_ARRAY",
        String.valueOf(Integer.MAX_VALUE - 8));
    MAX_INTS_PER_ARRAY = Integer.parseInt(max);
  }

  protected final int m_blockSize;

  private int m_watermark;
  private int m_free;
  private int m_head;

  int m_buffer[][];

  private int m_maxCapacity;

  private MemInitializer m_initializer;

  private boolean m_debug;

  private double m_growthFactor;

  private int m_maxBlocksPerArray;

  private int m_reservedBlocks;

  /**
   * @param maxBlocks number of blocks to reserve space for
   * @param blockSize record size in ints.
   */
  public BigBlockAllocator(int maxBlocks, int blockSize) {
    this(maxBlocks, blockSize, null);
  }

  /**
   * @param maxBlocks number of records to reserve space for
   * @param blockSize record size in ints.
   * @param initializer a callback to initialize newly allocated records
   */
  public BigBlockAllocator(int maxBlocks, int blockSize, MemInitializer initializer) {
    this(maxBlocks, blockSize, 0, initializer);
  }

  /**
   * @param maxBlocks number of records to reserve space for
   * @param blockSize record size in ints.
   * @param growthFactor determines by how much to grow buffer when it runs out
   *          of memory. 0 to disable growth
   */
  public BigBlockAllocator(int maxBlocks, int blockSize, double growthFactor) {
    this(maxBlocks, blockSize, growthFactor, null);
  }

  /**
   * @param maxBlocks number of records to reserve space for
   * @param blockSize record size in ints.
   * @param growthFactor determines by how much to grow buffer when it runs out
   *          of memory. 0 to disable growth
   * @param initializer a callback to initialize newly allocated records
   */
  public BigBlockAllocator(int maxBlocks, int blockSize, double growthFactor,
      MemInitializer initializer) {
    m_reservedBlocks = 1;
    m_head = -1;
    m_maxCapacity = maxBlocks + m_reservedBlocks;
    m_blockSize = blockSize;
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
    m_maxBlocksPerArray = MAX_INTS_PER_ARRAY / m_blockSize;
    int maxArrayUsage = m_maxBlocksPerArray * m_blockSize;
    long long_num_arrays = 1 + (size - 1) / maxArrayUsage;
    if (long_num_arrays > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Attempted to allocate " + long_num_arrays
          + " arrays of int[] , which is greated than Integer.MAX_VALUE (" + Integer.MAX_VALUE
          + ")");
    }

    long remains = size;
    int num_arrays = (int) long_num_arrays;
    m_buffer = new int[num_arrays][];
    m_head = -1;
    m_free = 0;
    m_watermark = 0;

    for (int i = 0; i < num_arrays; i++) {

      long allocate = Math.min(m_maxBlocksPerArray * m_blockSize, remains);
      if (allocate > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("Attempted to allocate " + allocate
            + " ints, which is greated than Integer.MAX_VALUE (" + Integer.MAX_VALUE + ")");
      }
      m_buffer[i] = new int[(int) allocate];
      remains -= allocate;
    }

    assert remains == 0;

    clear();
  }

  /**
   * Allocates a single block and returns a pointer to that block
   *
   * @return pointer to newly allocated block
   *
   * @throws net.yadan.banana.memory.OutOfMemoryException : if there are 0 free blocks
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

  @Override
  public void memCopy(int srcPtr, int srcPos, int dstPtr, int dstPos, int length) {
    assert srcPtr >= 0 : "Negative pointer : " + srcPtr;
    assert dstPtr >= 0 : "Negative pointer : " + srcPtr;
    assert length <= m_blockSize : "length > m_blockSize";
    assert srcPos + length <= m_blockSize : "src overflow";
    assert dstPos + length <= m_blockSize : "dst overflow";

    int srcArray = srcPtr / m_maxBlocksPerArray;
    srcPtr = srcPtr % m_maxBlocksPerArray;
    int dstArray = dstPtr / m_maxBlocksPerArray;
    dstPtr = dstPtr % m_maxBlocksPerArray;

    System.arraycopy(m_buffer[srcArray], srcPtr * m_blockSize + srcPos, m_buffer[dstArray], dstPtr
        * m_blockSize + dstPos, length);
  }


  @Override
  public void memSet(int pointer, int srcPos, int length, int value) {
    assert pointer >= 0 : "Negative pointer : " + pointer;
    assert length <= m_blockSize : "length > m_blockSize";
    assert srcPos + length <= m_blockSize : "overflow";

    int arr = pointer / m_maxBlocksPerArray;
    pointer = pointer % m_maxBlocksPerArray;

    int p = pointer * m_blockSize;
    for (int i = srcPos; i < srcPos + length; i++) {
      m_buffer[arr][p + i] = value;
    }
  }


  private void increaseSize() {
    int currentMaxBlocks = maxBlocks();
    int new_max_capacity = m_reservedBlocks
        + Math.max(currentMaxBlocks + 1, (int) (currentMaxBlocks * m_growthFactor));
    int used_blocks = usedBlocks() + m_reservedBlocks;
    int array_num = used_blocks / m_maxBlocksPerArray;
    int last_array_blocks_used = used_blocks % m_maxBlocksPerArray;
    int increase = new_max_capacity - used_blocks;
    if (last_array_blocks_used != 0) {
      int alloc = Math.min(increase + last_array_blocks_used, m_maxBlocksPerArray);
      int new_buffer[] = new int[alloc * m_blockSize];
      System.arraycopy(m_buffer[array_num], 0, new_buffer, 0, m_buffer[array_num].length);
      int old_size = m_buffer[array_num].length;
      m_buffer[array_num] = new_buffer;
      increase -= (alloc - old_size);
    }

    if (increase > 0) {
      int additional_required_arrays = 1 + (increase - 1) / m_maxBlocksPerArray;
      if (additional_required_arrays > 0) {
        int num_new_arrays = m_buffer.length + additional_required_arrays;
        int new_bufer[][] = new int[num_new_arrays][];
        System.arraycopy(m_buffer, 0, new_bufer, 0, m_buffer.length);
        for (int i = m_buffer.length; i < num_new_arrays; i++) {
          int alloc = increase;
          if (increase > m_maxBlocksPerArray) {
            alloc = m_maxBlocksPerArray;
          }
          new_bufer[i] = new int[alloc * m_blockSize];
          increase -= alloc;
        }

        m_buffer = new_bufer;
      }
    }

    assert increase == 0;

    m_maxCapacity = new_max_capacity;
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
  public short getUpperShort(int pointer, int offset) {
    assert pointer >= 0 : "Negative pointer : " + pointer;
    assert offset >= 0 : "Negative offset_in_data " + offset;
    assert offset < m_blockSize : String.format("offset >= m_blockSize : %d >= %d", offset, m_blockSize);

    int array_num = pointer / m_maxBlocksPerArray;
    int array_pointer = pointer % m_maxBlocksPerArray;
    return (short) (m_buffer[array_num][array_pointer * m_blockSize + offset] >>> 16);

  }

  @Override
  public short getLowerShort(int pointer, int offset) {
    assert pointer >= 0 : "Negative pointer : " + pointer;
    assert offset >= 0 : "Negative offset_in_data " + offset;
    assert offset < m_blockSize : String.format("offset >= m_blockSize : %d >= %d", offset, m_blockSize);

    int array_num = pointer / m_maxBlocksPerArray;
    int array_pointer = pointer % m_maxBlocksPerArray;
    return (short) (m_buffer[array_num][array_pointer * m_blockSize + offset]);
  }

  @Override
  public void setUpperShort(int pointer, int offset, int s) {
    assert pointer >= 0 : "Negative pointer : " + pointer;
    assert offset >= 0 : "Negative offset " + offset;
    assert offset < m_blockSize : String.format("offset >= m_blockSize : %d >= %d", offset, m_blockSize);

    int array_num = pointer / m_maxBlocksPerArray;
    int array_pointer = pointer % m_maxBlocksPerArray;
    int off = array_pointer * m_blockSize + offset;

    int lower = m_buffer[array_num][off] & 0x0000ffff;
    m_buffer[array_num][off] = (s << 16) | lower;
  }

  @Override
  public void setLowerShort(int pointer, int offset, int s) {
    assert pointer >= 0 : "Negative pointer : " + pointer;
    assert offset >= 0 : "Negative offset " + offset;
    assert offset < m_blockSize : String.format("offset >= m_blockSize : %d >= %d", offset, m_blockSize);

    int array_num = pointer / m_maxBlocksPerArray;
    int array_pointer = pointer % m_maxBlocksPerArray;
    int off = array_pointer * m_blockSize + offset;

    int upper = m_buffer[array_num][off] & 0xffff0000;
    m_buffer[array_num][off] = upper | (s & 0x0000ffff);
  }

  @Override
  public int getInt(int pointer, int offset_in_data) {
    assert pointer >= 0 : "Negative pointer : " + pointer;
    assert offset_in_data >= 0 : "Negative offset_in_data " + offset_in_data;
    assert offset_in_data < m_blockSize : String.format("offset_in_data >= m_blockSize : %d >= %d",
        offset_in_data, m_blockSize);
    int array_num = pointer / m_maxBlocksPerArray;
    int array_pointer = pointer % m_maxBlocksPerArray;
    return m_buffer[array_num][array_pointer * m_blockSize + offset_in_data];
  }

  @Override
  public void setInt(int pointer, int offset_in_data, int data) {
    assert pointer >= 0 : "Negative pointer : " + pointer;
    int buffer[] = m_buffer[pointer / m_maxBlocksPerArray];
    pointer = pointer % m_maxBlocksPerArray;

    assert offset_in_data >= 0 : "Negative offset_in_data " + offset_in_data;
    assert offset_in_data < m_blockSize : String.format("offset_in_data >= m_blockSize : %d >= %d",
        offset_in_data, m_blockSize);

    buffer[pointer * m_blockSize + offset_in_data] = data;
  }

  @Override
  public void setInts(int pointer, int dst_offset_in_record,
      int src_data[], int src_pos, int length) {

    assert pointer >= 0 : "Negative pointer : " + pointer;
    int buffer[] = m_buffer[pointer / m_maxBlocksPerArray];
    pointer = pointer % m_maxBlocksPerArray;

    assert src_pos >= 0 : "Negative src_pos";
    assert src_pos + length <= src_data.length : String.format(
        "src_pos + length > src_data.length : %d + %d > %d", src_pos, length, src_data.length);
    assert pointer * m_blockSize + src_pos + length <= buffer.length : String.format(
        "pointer + src_pos + length > m_buffer.length  : %d + %d + %d >= %d", pointer,
        src_pos, length, buffer.length);
    assert dst_offset_in_record + length <= m_blockSize : String.format(
        "dst_offset_in_record + length > m_blockSize   : %d + %d >= %d", dst_offset_in_record,
        length, m_blockSize);

    System.arraycopy(src_data, src_pos, buffer, pointer * m_blockSize + dst_offset_in_record,
        length);
  }

  @Override
  public void getInts(int pointer, int src_offset_in_record,
      int dst_data[], int dst_pos, int length) {

    assert pointer >= 0 : "Negative pointer : " + pointer;
    int array_num = pointer / m_maxBlocksPerArray;
    int buffer[] = m_buffer[array_num];
    int array_pointer = pointer % m_maxBlocksPerArray;

    assert array_pointer * m_blockSize < buffer.length : String.format(
        "pointer >= m_buffer.length : %d < 0", array_pointer, buffer.length);
    assert src_offset_in_record >= 0 : String.format("src_offset_in_record < 0 : %d < 0",
        src_offset_in_record);
    assert src_offset_in_record < m_blockSize : String.format(
        "src_offset_in_record >= m_blockSize : %d >= %d", src_offset_in_record, m_blockSize);
    assert dst_pos >= 0 : String.format("dst_pos < 0 : %d", dst_pos);
    assert dst_pos + length <= dst_data.length : String.format(
        "dst_pos + length > dst_data.length : %d + %d >= %d", dst_pos, length, dst_data.length);

    System.arraycopy(buffer, array_pointer * m_blockSize + src_offset_in_record, dst_data, dst_pos,
        length);
  }

  @Override
  public void getBuffer(int pointer, int src_offset_in_record, IBuffer dst, int length) {
    getInts(pointer, src_offset_in_record, dst.array(), 0, length);
    dst.setUsed(length);
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
      s.append(String.format(
          "BigIntBlockAllocator %s/%s records of %d ints used, total ints allocated %d",
          usedBlocks(), maxBlocks(), m_blockSize, (long) m_maxCapacity * m_blockSize));
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

  private int next(int pointer) {
    return getInt(pointer, 0);
  }

  private void set_next(int pointer, int next) {
    setInt(pointer, 0, next);
  }

  @Override
  public void clear() {
    m_head = -1;
    m_watermark = m_reservedBlocks;
    m_free = 0;
    set_next(0, -1);
  }

  public String debugString() {
    StringBuilder sb = new StringBuilder();
    for (int buffer[] : m_buffer) {
      for (int d : buffer) {
        sb.append(String.format("%05X", d & 0xFFFFF)).append(' ');
      }
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
    long mem = 0;
    for (int arr[] : m_buffer) {
      mem += arr.length;
    }
    return mem * 4;
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

  @Override
  public void setChars(int pointer, int dst_offset, char[] src_data, int src_pos, int num_chars) {
    if (num_chars == 0) {
      return;
    }
    int numInts = 1 + (num_chars - 1) / 2; // ceil(num_chars/2)
    for (int i = dst_offset, src_index = src_pos, num_copied = 0; i < dst_offset + numInts; i++, src_index += 2) {
      setUpperShort(pointer, i, src_data[src_index]);
      num_copied++;
      if (num_copied < num_chars) {
        setLowerShort(pointer, i, src_data[src_index + 1]);
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
}
