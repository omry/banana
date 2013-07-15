/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory;

public interface IBuffer {

  /**
   * @return the number of ints used in this buffer
   */
  public int size();

  /**
   * @return growth factor
   */
  public double getGrowthFactor();

  /**
   * @param growthFactor
   */
  public void setGrowthFactor(double growthFactor);

  /**
   * @return the number of ints this IBuffer can contain
   */
  public int capacity();

  /**
   * Resets the content of the buffer by setting the size to 0.
   */
  public void reset();

  public short getUpperShort(int offset);

  public short getLowerShort(int offset);

  public void setUpperShort(int offset, int v);

  public void setLowerShort(int offset, int v);

  public int getInt(int offset);

  public void setInt(int offset, int data);

  public long getLong(int offset);

  public void setLong(int offset, long data);

  public void setInts(int dst_offset, int src_data[], int src_pos, int length);

  public void getInts(int src_offset, int dst_data[], int dst_pos, int length);

  public void setChars(int dst_offset, char src_data[], int src_pos, int length);

  public void setChars(int dst_offset, char src_data[]);

  public void getChars(int src_offset, char dst_data[], int dst_pos, int length);

  public void appendInt(int v);

  public void appendLong(long v);

  public void appendInts(int src_data[]);

  public void appendInts(int src_data[], int src_pos, int length);

  public void ensureCapacity(int numInts);

  @Override
  public int hashCode();

  public boolean equals(IMemAllocator mem, int pointer, int start_offset, int length);

  public int[] array();

  public void appendChars(char[] src_data);

  public void appendChars(char[] src_data, int src_pos, int length);

  public void setUsed(int used);
}
