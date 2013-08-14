/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.stack;

import net.yadan.banana.DebugLevel;
import net.yadan.banana.DefaultFormatter;
import net.yadan.banana.Formatter;
import net.yadan.banana.ICollection;
import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.OffsetPrimitiveAccess;
import net.yadan.banana.memory.malloc.ChainedAllocator;

/**
 * A stack backed by a single int[] array. Stack supports variable size frames
 * 
 * @author omry
 * @created Apr 22, 2013
 */
public class Stack implements ICollection {

  private final static int NEXT_OFFSET = 0;
  private final static int DATA_OFFSET = 1;
  private IMemAllocator m_memory;

  private int m_head;
  private int m_size;
  private DebugLevel m_debugLevel = DebugLevel.NONE;
  private Formatter m_formatter;

  public Stack(int numElements, int recordSize) {
    if (recordSize <= 0)
      throw new IllegalArgumentException("Non positive record size " + recordSize);
    m_memory = new ChainedAllocator(numElements, recordSize + DATA_OFFSET);
    m_formatter = new DefaultFormatter();
    m_head = -1;
    m_size = 0;
  }

  public void push(int size) {
    int n = m_memory.malloc(size + DATA_OFFSET);
    m_memory.setInt(n, NEXT_OFFSET, m_head);
    m_head = n;
    m_size++;
  }

  public void push(IBuffer data) {
    push(data.size());
    m_memory.setInts(m_head, DATA_OFFSET, data.array(), 0, data.size());
  }

  public void pop() {
    ensure_not_empty();
    int next = m_memory.getInt(m_head, NEXT_OFFSET);
    m_memory.free(m_head);
    m_head = next;
    m_size--;
  }

  public void setInt(int data, int offset_in_data) {
    ensure_not_empty();
    m_memory.setInt(m_head, DATA_OFFSET + offset_in_data, data);
  }

  public void setLong(long data, int offset_in_data) {
    ensure_not_empty();
    m_memory.setLong(m_head, DATA_OFFSET + offset_in_data, data);
  }

  public void set_ints(int src_data[], int src_pos, int length, int dst_offset_in_record) {
    ensure_not_empty();
    m_memory.setInts(m_head, DATA_OFFSET + dst_offset_in_record, src_data, src_pos, length);
  }

  public int getInt(int offset_in_data) {
    ensure_not_empty();
    return m_memory.getInt(m_head, DATA_OFFSET + offset_in_data);
  }

  public long getLong(int offset_in_data) {
    ensure_not_empty();
    return m_memory.getLong(m_head, DATA_OFFSET + offset_in_data);
  }

  public void getInts(int src_offset_in_record, int dst_data[], int dst_pos, int length) {
    ensure_not_empty();
    m_memory.getInts(m_head, DATA_OFFSET + src_offset_in_record, dst_data, dst_pos, length);
  }

  @Override
  public boolean isEmpty() {
    return m_head == -1;
  }

  public int availableBlocks() {
    return m_memory.freeBlocks();
  }

  public int usedBlocks() {
    return m_memory.usedBlocks();
  }

  public int maxBlocks() {
    return m_memory.maxBlocks();
  }

  public int blockSize() {
    return m_memory.blockSize() - DATA_OFFSET;
  }

  private void ensure_not_empty() {
    if (isEmpty()) {
      throw new IllegalStateException("Stack empty");
    }
  }

  @Override
  public void clear() {
    while (!isEmpty()) {
      pop();
    }
  }

  @Override
  public int size() {
    return m_size;
  }

  @Override
  public long computeMemoryUsage() {
    return m_memory.computeMemoryUsage();
  }

  @Override
  public void setDebug(DebugLevel level) {
    m_debugLevel = level;
  }

  @Override
  public DebugLevel getDebug() {
    return m_debugLevel;
  }

  @Override
  public void setFormatter(Formatter formatter) {
    m_formatter = formatter;
  }

  @Override
  public Formatter getFormatter() {
    return m_formatter;
  }

  @Override
  public String toString() {
    try {
      OffsetPrimitiveAccess prim = new OffsetPrimitiveAccess(m_memory, DATA_OFFSET);
      StringBuilder sb = new StringBuilder("Stack (" + size() + " records)");
      switch (m_debugLevel) {
      case DEBUG_CONTENT: {
        sb.append("\n");
        int n = m_head;
        while (n != -1) {
          int next = m_memory.getInt(n, NEXT_OFFSET);
          String st;
          try {
            st = m_formatter.format(prim, n);
          } catch (RuntimeException e) {
            st = "\t" + e.getClass().getSimpleName() + " : " + e.getMessage();
          }
          sb.append("\t").append(st);
          if (next != -1) {
            sb.append("\n");
          }
          n = next;
        }
      }
        break;
      case DEBUG_STRUCTURE: {
        sb.append("\n");
        int n = m_head;
        while (n != -1) {
          int next = m_memory.getInt(n, NEXT_OFFSET);
          sb.append("#").append(n).append(" : ");
          String st;
          try {
            st = "N #" + next + " : " + m_formatter.format(prim, n);
          } catch (RuntimeException e) {
            st = e.getClass().getSimpleName() + " : " + e.getMessage();
          }
          sb.append("\t").append(st);
          if (next != -1) {
            sb.append("\n");
          }
          n = next;
        }
      }
        break;
      case NONE:
        break;
      }
      return sb.toString();
    } catch (RuntimeException e) {
      return "Exception in toString() : " + e.getClass().getSimpleName() + " : " + e.getMessage();
    }
  }
}
