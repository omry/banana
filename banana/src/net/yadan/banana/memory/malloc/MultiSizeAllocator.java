/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory.malloc;

import java.util.Arrays;

import net.yadan.banana.memory.IBlockAllocator;
import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.MemInitializer;
import net.yadan.banana.memory.block.BlockAllocator;


/**
 * Variable length int's allocator.
 *
 * @author omry
 * @created May 2, 2013
 */
public class MultiSizeAllocator implements IMemAllocator {

  private int[] m_sizes;

  private IBlockAllocator m_allocators[];

  private int m_reservedBits;
  private int m_allocationBits;
  private int m_maxAllocationPointer;

  public MultiSizeAllocator(int numInitialBlocks, int sizes[]) {
    this(numInitialBlocks, sizes, 0);
  }

  public MultiSizeAllocator(int numInitialBlocks, int sizes[], double growthFactor) {
    m_sizes = sizes;
    // if you don't have two sizes just use IntAllocator
    if (m_sizes.length < 2)
      throw new IllegalArgumentException("Need at least two sizes");

    Arrays.sort(sizes);
    m_allocators = new IBlockAllocator[sizes.length];
    for (int i = 0; i < sizes.length; i++) {
      m_allocators[i] = new BlockAllocator(numInitialBlocks, sizes[i], growthFactor);
    }

    m_reservedBits = (int) Math.ceil(Math.log(sizes.length) / Math.log(2));
    m_allocationBits = 32 - m_reservedBits;
    m_maxAllocationPointer = (int) Math.pow(2, m_allocationBits);
  }

  @Override
  public int malloc(int size) {
    int idx = findAllocatorFor(size);
    IBlockAllocator alloc = m_allocators[idx];
    int pointer = alloc.malloc();
    if (pointer > m_maxAllocationPointer) {
      throw new IllegalStateException("Pointer value too large");
    }

    return encodePointer(idx, pointer);
  }

  final int encodePointer(int idx, int pointer) {
    return idx << m_allocationBits | pointer;
  }

  @Override
  public void free(int pointer) {
    int idx = getSizeIndex(pointer);
    pointer = extractPointer(pointer);
    m_allocators[idx].free(pointer);
  }

  @Override
  public int getInt(int pointer, int offset_in_data) {
    int idx = getSizeIndex(pointer);
    pointer = extractPointer(pointer);
    return m_allocators[idx].getInt(pointer, offset_in_data);
  }

  @Override
  public void setInt(int pointer, int offset_in_data, int data) {
    int idx = getSizeIndex(pointer);
    pointer = extractPointer(pointer);
    m_allocators[idx].setInt(pointer, offset_in_data, data);
  }

  @Override
  public void setInts(int pointer, int dst_offset_in_record, int src_data[], int src_pos, int length) {
    int idx = getSizeIndex(pointer);
    pointer = extractPointer(pointer);
    m_allocators[idx].setInts(pointer, dst_offset_in_record, src_data, src_pos, length);
  }

  @Override
  public void getInts(int pointer, int src_offset_in_record, int dst_data[], int dst_pos, int length) {
    int idx = getSizeIndex(pointer);
    pointer = extractPointer(pointer);
    m_allocators[idx].getInts(pointer, src_offset_in_record, dst_data, dst_pos, length);
  }

  @Override
  public void getBuffer(int pointer, int src_offset_in_record, IBuffer dst, int length) {
    getInts(pointer, src_offset_in_record, dst.array(), 0, length);
    dst.setUsed(length);
  }

  @Override
  public long getLong(int pointer, int offset_in_data) {
    int idx = getSizeIndex(pointer);
    pointer = extractPointer(pointer);
    return m_allocators[idx].getLong(pointer, offset_in_data);
  }

  @Override
  public void setLong(int pointer, int offset_in_data, long data) {
    int idx = getSizeIndex(pointer);
    pointer = extractPointer(pointer);
    m_allocators[idx].setLong(pointer, offset_in_data, data);
  }

  final int extractPointer(int pointer) {
    return pointer & ((1 << m_allocationBits) - 1);
  }

  public int getSizeIndex(int pointer) {
    assert pointer != 0 : "Invalid pointer " + pointer;
    assert pointer != -1 : "Invalid pointer " + pointer;
    return pointer >>> m_allocationBits;
  }

  public int getReservedBits() {
    return m_reservedBits;
  }

  int findAllocatorFor(int size) {
    int i = Arrays.binarySearch(m_sizes, size);
    if (i == -(m_sizes.length + 1)) {
      throw new IllegalArgumentException("Requested an allocation of unsupported size " + size
          + ", max " + m_sizes[m_sizes.length - 1]);
    }
    if (i < 0) {
      return (-i) - 1;
    }

    return i;

  }

  @Override
  public int computeMemoryUsageFor(int size) {
    return m_allocators[findAllocatorFor(size)].blockSize();
  }

  @Override
  public boolean isDebug() {
    return m_allocators[0].isDebug();
  }

  @Override
  public void setDebug(boolean debug) {
    for (IBlockAllocator allocator : m_allocators) {
      allocator.setDebug(debug);
    }
  }

  @Override
  public void setInitializer(MemInitializer initializer) {
    for (IBlockAllocator allocator : m_allocators) {
      allocator.setInitializer(initializer);
    }
  }

  @Override
  public int usedBlocks() {
    int used = 0;
    for (IBlockAllocator m : m_allocators) {
      used += m.usedBlocks();
    }
    return used;
  }

  @Override
  public int blockSize() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int maxBlocks() {
    int max = 0;
    for (IBlockAllocator m : m_allocators) {
      max += m.maxBlocks();
    }
    return max;
  }

  @Override
  public int freeBlocks() {
    int free = 0;
    for (IBlockAllocator m : m_allocators) {
      free += m.freeBlocks();
    }
    return free;
  }

  @Override
  public void clear() {
    for (IBlockAllocator allocator : m_allocators) {
      allocator.clear();
    }
  }

  @Override
  public long computeMemoryUsage() {
    long mem = 0;
    for (IBlockAllocator allocator : m_allocators) {
      mem += allocator.computeMemoryUsage();
    }
    return mem;
  }

  @Override
  public void setGrowthFactor(double d) {
    for (IBlockAllocator allocator : m_allocators) {
      allocator.setGrowthFactor(d);
    }
  }

  @Override
  public double getGrowthFactor() {
    return m_allocators[0].getGrowthFactor();
  }

  @Override
  public int maximumCapacityFor(int pointer) {
    int idx = getSizeIndex(pointer);
    return m_allocators[idx].blockSize();
  }

  @Override
  public int realloc(int pointer, int size) {
    // TODO
    throw new UnsupportedOperationException();
  }

  @Override
  public IBlockAllocator getBlocks() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String pointerDebugString(int pointer) {
    int idx = getSizeIndex(pointer);
    pointer = extractPointer(pointer);
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    int bs = m_allocators[idx].blockSize();
    for (int i = 0; i < bs; i++) {
      sb.append(m_allocators[idx].getInt(pointer, i));
      if (i + 1 < bs) {
        sb.append(",");
      }
    }
    sb.append("]");
    return "Pointer from allocator " + idx + " " + sb.toString();
  }

  @Override
  public void initialize(int pointer) {
    int idx = getSizeIndex(pointer);
    pointer = extractPointer(pointer);
    m_allocators[idx].initialize(pointer);
  }

  @Override
  public void memSet(int pointer, int srcPos, int length, int value) {
    int idx = getSizeIndex(pointer);
    pointer = extractPointer(pointer);
    m_allocators[idx].memSet(pointer, srcPos, length, value);
  }

  @Override
  public short getUpperShort(int pointer, int offset) {
    int idx = getSizeIndex(pointer);
    pointer = extractPointer(pointer);
    return m_allocators[idx].getUpperShort(pointer, offset);
  }

  @Override
  public short getLowerShort(int pointer, int offset) {
    int idx = getSizeIndex(pointer);
    pointer = extractPointer(pointer);
    return m_allocators[idx].getLowerShort(pointer, offset);
  }

  @Override
  public void setUpperShort(int pointer, int offset, int s) {
    int idx = getSizeIndex(pointer);
    pointer = extractPointer(pointer);
    m_allocators[idx].setUpperShort(pointer, offset, s);
  }

  @Override
  public void setLowerShort(int pointer, int offset, int s) {
    int idx = getSizeIndex(pointer);
    pointer = extractPointer(pointer);
    m_allocators[idx].setLowerShort(pointer, offset, s);
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
    int idx = getSizeIndex(pointer);
    pointer = extractPointer(pointer);
    m_allocators[idx].setChars(pointer, dst_offset, src_data, src_pos, num_chars);
  }

  @Override
  public void getChars(int pointer, int src_offset, char[] dst_data, int dst_pos, int num_chars) {
    int idx = getSizeIndex(pointer);
    pointer = extractPointer(pointer);
    m_allocators[idx].getChars(pointer, src_offset, dst_data, dst_pos, num_chars);
  }
}
