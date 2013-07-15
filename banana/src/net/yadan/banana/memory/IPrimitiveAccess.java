/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory;

public interface IPrimitiveAccess {

  /**
   * @param pointer pointer to read int from
   * @param offset_in_data offset in data to read from
   */
  public int getInt(int pointer, int offset_in_data);

  /**
   * @param pointer pointer to read int from
   * @param offset_in_data offset in data to write to
   * @param data the int to write
   */
  public void setInt(int pointer, int offset_in_data, int data);

  public short getUpperShort(int pointer, int offset);

  public short getLowerShort(int pointer, int offset);

  public void setUpperShort(int pointer, int offset, int s);

  public void setLowerShort(int pointer, int offset, int s);

  /**
   * @param pointer pointer to read long from
   * @param offset_in_data offset in data to read from
   */
  public long getLong(int pointer, int offset_in_data);

  /**
   * @param pointer pointer to read long from
   * @param offset_in_data offset in data to write to
   * @param data the long to write
   */
  public void setLong(int pointer, int offset_in_data, long data);

  /**
   * Copy an int[] array into the buffer
   *
   * @param pointer pointer to a previously allocated block
   * @param dst_offset_in_record target offset inside block
   * @param src_data source data
   * @param src_pos source position
   * @param length number of ints to copy
   */
  public void setInts(int pointer, int dst_offset_in_record,
      int src_data[], int src_pos, int length);


  /**
   * Copy an int[] array from the buffer
   *
   * @param pointer pointer to a previously allocated block
   * @param src_offset_in_record source offset in specified block
   * @param dst_data destination array
   * @param dst_pos destination offset
   * @param length number of ints to copy
   */
  public void getInts(int pointer, int src_offset_in_record,
      int dst_data[], int dst_pos, int length);

  //
  // /**
  // * Copy a char[] array into the buffer
  // *
  // * @param pointer
  // * destination pointer
  // * @param dst_offset
  // * offset in dest pointer (in ints)
  // * @param src_data
  // * source char[] data
  // * @param src_pos
  // * source position to start copy from (in chars)
  // * @param length
  // * number of chars to copy
  // */
  // public void setChars(int pointer, int dst_offset, char src_data[], int
  // src_pos, int length);
  //
  // /**
  // * Copy a char[] array from the buffer
  // *
  // * @param pointer
  // * source pointer
  // * @param src_offset
  // * offset in source pointer (int ints)
  // * @param dst_data
  // * dest char[] data
  // * @param dst_pos
  // * dest position to copy to (in chars)
  // * @param length
  // * number of chars to copy
  // */
  // public void getChars(int pointer, int src_offset, char dst_data[], int
  // dst_pos, int length);

  /**
   * @param pointer
   *          pointer to a previously allocated block
   * @param src_offset_in_record
   *          source offset in specified block
   * @param dst
   *          destination buffer to copy into
   * @param length
   *          number of ints to copy
   */
  public void getBuffer(int pointer, int src_offset_in_record, IBuffer dst, int length);

  /**
   * Returns the maximum ints capacity for the specified pointer. depending on
   * the allocator this may be larger than requested size
   */
  public int maximumCapacityFor(int pointer);
}
