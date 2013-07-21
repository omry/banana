/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory;

public interface IPrimitiveAccess {

  /**
   * Gets the lower 16 bit short from the specified offset
   * 
   * @param pointer
   *          pointer to read from
   * @param offset
   *          offset of int to get short from
   * @return
   */
  public short getLowerShort(int pointer, int offset);

  /**
   * Sets the lower (Least significant) 16 bit short in the int at the offset
   * 
   * @param pointer
   *          pointer to write into
   * @param offset
   *          offset to write to
   * 
   * @param s
   *          short to write
   */
  public void setLowerShort(int pointer, int offset, int s);


  /**
   * Gets the upper 16 bit short from the specified offset
   * 
   * @param pointer
   *          pointer to read from
   * @param offset
   *          offset of int to get short from
   * @return
   */
  public short getUpperShort(int pointer, int offset);

  /**
   * Sets the upper (Most significant) 16 bit short in the int at the offset
   * 
   * @param pointer
   *          pointer to write into
   * @param offset
   *          offset to write to
   * 
   * @param s
   *          short to write
   */
  public void setUpperShort(int pointer, int offset, int s);
  
  /**
   * Gets float (32bit) from the int at the specified offset
   * 
   * @param pointer
   *          pointer to read from
   * @param offset
   *          offset in pointer to read from
   * 
   * @return
   */
  public float getFloat(int pointer, int offset);
  
  /**
   * Sets float (32bit) on the int at the specified offset
   * 
   * @param pointer
   *          pointer to write to
   * @param offset
   *          offset in pointer to write to
   * @param f
   *          float value to write
   */
  public void setFloat(int pointer, int offset, float f);

  /**
   * @param pointer
   *          pointer to read int from
   * @param offset_in_data
   *          offset in data to read from
   */
  public int getInt(int pointer, int offset_in_data);

  /**
   * @param pointer
   *          pointer to read int from
   * @param offset_in_data
   *          offset in data to write to
   * @param data
   *          the int to write
   */
  public void setInt(int pointer, int offset_in_data, int data);

  /**
   * Gets long (64bit) from the 2 ints starting at the specified offset
   * 
   * @param pointer
   *          pointer to read long from
   * @param offset_in_data
   *          offset in data to read from
   */
  public long getLong(int pointer, int offset_in_data);

  /**
   * Sets long (64bit) onto the 2 ints starting at the specified offset
   * 
   * @param pointer
   *          pointer to read long from
   * @param offset_in_data
   *          offset in data to write to
   * @param data
   *          the long to write
   */
  public void setLong(int pointer, int offset_in_data, long data);

  /**
   * Gets double (64bit) from the 2 ints starting at the specified offset
   * 
   * @param pointer
   *          pointer to read double from
   * @param offset_in_data
   *          offset in data to read from
   */
  public double getDouble(int pointer, int offset_in_data);

  /**
   * Sets double (64bit) onto the 2 ints starting at the specified offset
   * 
   * @param pointer
   *          pointer to read double from
   * @param offset_in_data
   *          offset in data to write to
   * @param data
   *          the double to write
   */
  public void setDouble(int pointer, int offset_in_data, double data);

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

  /**
   * Copy a char[] array into the buffer, each two chars will be copied into a
   * single int in the underlying array.
   * 
   * @param pointer
   *          destination pointer
   * @param dst_offset
   *          offset in dest pointer (in ints)
   * @param src_data
   *          source char[] data
   * @param src_pos
   *          source position to start copy from (in chars)
   * @param num_chars
   *          number of chars to copy
   */
  public void setChars(int pointer, int dst_offset, char src_data[], int src_pos, int num_chars);

  /**
   * Copy a char[] array from the buffer, each two chars will be copied from a
   * single int in the underlying array.
   * 
   * @param pointer
   *          source pointer
   * @param src_offset
   *          offset in source pointer (int ints)
   * @param dst_data
   *          dest char[] data
   * @param dst_pos
   *          dest position to copy to (in chars)
   * @param num_chars
   *          number of chars to copy
   */
  public void getChars(int pointer, int src_offset, char dst_data[], int dst_pos, int num_chars);

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
