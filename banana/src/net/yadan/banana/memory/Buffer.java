/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory;

public class Buffer implements IBuffer {
  // TODO: Use BlockAllocator as an underlying storage
  private int m_buffer[];
  private int m_usedSize;
  private double m_growthFactor;

  public Buffer(int a[], double growthFactor) {
    this(a.length, growthFactor);
    setInts(0, a, 0, a.length);
  }

  public Buffer(short a[], double growthFactor) {
    this(1 + (a.length - 1) / 2, growthFactor);
    int numInts = 1 + (a.length - 1) / 2;
    for (int i = 0; i < numInts; i++) {
      int elementIndex = i * 2;
      setUpperShort(i, a[elementIndex]);
      if (a.length < elementIndex + 1) {
        setLowerShort(i, a[elementIndex + 1]);
      }
    }
  }

  public Buffer(int initialMaxSize) {
    this(initialMaxSize, 2);
  }

  public Buffer(int initialMaxSize, double growthFactor) {
    m_growthFactor = growthFactor;
    m_buffer = new int[initialMaxSize];
    m_usedSize = 0;
  }

  @Override
  public double getGrowthFactor() {
    return m_growthFactor;
  }

  @Override
  public void setGrowthFactor(double growthFactor) {
    m_growthFactor = growthFactor;
  }

  @Override
  public int size() {
    return m_usedSize;
  }

  @Override
  public void reset() {
    m_usedSize = 0;
  }

  @Override
  public void ensureCapacity(int numInts) {
    int capacity = capacity();
    if (capacity < numInts) {
      if (m_growthFactor == 0) {
        throw new OutOfMemoryException("Buffer is configured to not grow (growthFactor = 0)");
      }

      int newBuf[] = new int[(int) (capacity * m_growthFactor)];
      System.arraycopy(m_buffer, 0, newBuf, 0, m_buffer.length);
      m_buffer = newBuf;
    }
  }

  @Override
  public int capacity() {
    return m_buffer.length;
  }

  @Override
  public short getUpperShort(int offset) {
    return (short) (m_buffer[offset] >>> 16);
  }

  @Override
  public short getLowerShort(int offset) {
    return (short) (m_buffer[offset]);
  }

  @Override
  public void setUpperShort(int offset, int v) {
    int newSize = Math.max(m_usedSize, offset + 1);
    int lower = newSize > m_usedSize ? 0 : m_buffer[offset] & 0x0000ffff;
    m_buffer[offset] = (v << 16) | lower;
    m_usedSize = newSize;
  }

  @Override
  public void setLowerShort(int offset, int v) {
    int newSize = Math.max(m_usedSize, offset + 1);

    int upper = newSize > m_usedSize ? 0 : m_buffer[offset] & 0xffff0000;
    m_buffer[offset] = upper | (v & 0x0000ffff);
    m_usedSize = newSize;
  }


  @Override
  public int getInt(int offset) {
    return m_buffer[offset];
  }

  @Override
  public void setInt(int offset, int v) {
    m_buffer[offset] = v;
    m_usedSize = Math.max(m_usedSize, offset + 1);
  }

  @Override
  public long getLong(int offset) {
    int ilower = m_buffer[offset + 1];
    int iupper = m_buffer[offset + 0];
    long lower = 0x00000000FFFFFFFFL & ilower;
    long upper = ((long) iupper) << 32;
    long ret = upper | lower;
    return ret;
  }

  @Override
  public void setLong(int offset, long v) {
    m_buffer[offset + 0] = (int) (v >> 32);
    m_buffer[offset + 1] = (int) (v);
    m_usedSize = Math.max(m_usedSize, offset + 2);
  }

  @Override
  public void setInts(int dst_offset, int[] src_data, int src_pos, int length) {
    System.arraycopy(src_data, src_pos, m_buffer, dst_offset, length);
    m_usedSize = Math.max(m_usedSize, dst_offset + length);
  }

  @Override
  public void getInts(int src_offset, int[] dst_data, int dst_pos, int length) {
    System.arraycopy(m_buffer, src_offset, dst_data, dst_pos, length);
  }

  @Override
  public void appendInt(int v) {
    setInt(m_usedSize, v);
  }

  @Override
  public void appendLong(long v) {
    setLong(m_usedSize, v);
  }


  @Override
  public void appendInts(int[] src_data) {
    appendInts(src_data, 0, src_data.length);
  }

  @Override
  public void appendInts(int[] src_data, int src_pos, int length) {
    setInts(m_usedSize, src_data, src_pos, length);
  }

  @Override
  public int hashCode() {
    int h = 1;
    for (int i = 0; i < size(); i++) {
      h = 31 * h + m_buffer[i];
    }
    return h;
  }

  @Override
  public boolean equals(IMemAllocator mem, int pointer, int start_offset, int length) {
    if (length != size()) {
      return false;
    }

    for (int i = start_offset; i < start_offset + length; i++) {
      int v1 = mem.getInt(pointer, i);
      int v2 = getInt(i - start_offset);
      if (v1 != v2) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int[] array() {
    return m_buffer;
  }

  @Override
  public void setChars(int dst_offset, char[] src_data) {
    setChars(dst_offset, src_data, 0, src_data.length);
  }

  @Override
  public void setChars(int dst_offset, char[] src_data, int src_pos, int length) {
    if (length == 0) {
      return;
    }
    int numInts = 1 + (length - 1) / 2;
    for (int i = 0; i < numInts; i++) {
      int elementIndex = i * 2;
      setUpperShort(dst_offset + i, src_data[src_pos + elementIndex]);
      if (elementIndex + 1 < length) {
        setLowerShort(dst_offset + i, src_data[src_pos + elementIndex + 1]);
      }
    }

    m_usedSize = Math.max(m_usedSize, dst_offset + numInts);
  }

  @Override
  public void getChars(int src_offset, char[] dst_data, int dst_pos, int length) {
    if (length == 0) {
      return;
    }

    int numInts = 1 + (length - 1) / 2;
    for (int i = 0; i < numInts; i++) {
      int elementIndex = i * 2;
      dst_data[dst_pos + elementIndex] = (char) getUpperShort(src_offset + i);
      if (elementIndex + 1 < length) {
        dst_data[dst_pos + elementIndex + 1] = (char) getLowerShort(src_offset + i);
      }
    }
  }


  @Override
  public void appendChars(char[] src_data) {
    appendChars(src_data, 0, src_data.length);
  }

  @Override
  public void appendChars(char[] src_data, int src_pos, int length) {
    setChars(m_usedSize, src_data, src_pos, length);
  }

  @Override
  public void setUsed(int used) {
    assert used <= capacity();
    m_usedSize = used;
  }

  @Override
  public String toString() {
    return "Buffer " + m_usedSize + " / " + capacity() + " used";
  }
}
