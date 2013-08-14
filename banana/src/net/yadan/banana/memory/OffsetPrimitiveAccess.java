/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory;

public class OffsetPrimitiveAccess implements IPrimitiveAccess {

  private IPrimitiveAccess m_parent;
  private int m_baseOffset;

  public OffsetPrimitiveAccess(IPrimitiveAccess parent, int baseOffset) {
    m_parent = parent;
    m_baseOffset = baseOffset;
  }

  @Override
  public short getLowerShort(int pointer, int offset) {
    return m_parent.getLowerShort(pointer, m_baseOffset + offset);
  }

  @Override
  public void setLowerShort(int pointer, int offset, int s) {
    m_parent.setLowerShort(pointer, m_baseOffset + offset, s);
  }

  @Override
  public short getUpperShort(int pointer, int offset) {
    return m_parent.getUpperShort(pointer, m_baseOffset + offset);
  }

  @Override
  public void setUpperShort(int pointer, int offset, int s) {
    m_parent.setUpperShort(pointer, m_baseOffset + offset, s);
  }

  @Override
  public float getFloat(int pointer, int offset) {
    return m_parent.getFloat(pointer, m_baseOffset + offset);
  }

  @Override
  public void setFloat(int pointer, int offset, float f) {
    m_parent.setFloat(pointer, m_baseOffset + offset, f);
  }

  @Override
  public int getInt(int pointer, int offset_in_data) {
    return m_parent.getInt(pointer, m_baseOffset + offset_in_data);
  }

  @Override
  public void setInt(int pointer, int offset_in_data, int data) {
    m_parent.setInt(pointer, m_baseOffset + offset_in_data, data);
  }

  @Override
  public long getLong(int pointer, int offset_in_data) {
    return m_parent.getLong(pointer, m_baseOffset + offset_in_data);
  }

  @Override
  public void setLong(int pointer, int offset_in_data, long data) {
    m_parent.setLong(pointer, m_baseOffset + offset_in_data, data);
  }

  @Override
  public double getDouble(int pointer, int offset_in_data) {
    return m_parent.getDouble(pointer, m_baseOffset + offset_in_data);
  }

  @Override
  public void setDouble(int pointer, int offset_in_data, double data) {
    m_parent.setDouble(pointer, m_baseOffset + offset_in_data, data);
  }

  @Override
  public void setInts(int pointer, int dst_offset_in_record, int[] src_data, int src_pos, int length) {
    m_parent.setInts(pointer, m_baseOffset + dst_offset_in_record, src_data, src_pos, length);
  }

  @Override
  public void getInts(int pointer, int src_offset_in_record, int[] dst_data, int dst_pos, int length) {
    m_parent.getInts(pointer, m_baseOffset + src_offset_in_record, dst_data, dst_pos, length);
  }

  @Override
  public void setChars(int pointer, int dst_offset, char[] src_data, int src_pos, int num_chars) {
    m_parent.setChars(pointer, m_baseOffset + dst_offset, src_data, src_pos, num_chars);
  }

  @Override
  public void getChars(int pointer, int src_offset, char[] dst_data, int dst_pos, int num_chars) {
    m_parent.getChars(pointer, m_baseOffset + src_offset, dst_data, dst_pos, num_chars);
  }

  @Override
  public void getBuffer(int pointer, int src_offset_in_record, IBuffer dst, int length) {
    m_parent.getBuffer(pointer, m_baseOffset + src_offset_in_record, dst, length);
  }

  @Override
  public int maximumCapacityFor(int pointer) {
    return m_parent.maximumCapacityFor(pointer) - m_baseOffset;
  }
}
