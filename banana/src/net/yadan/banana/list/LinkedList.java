/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.list;

import net.yadan.banana.DebugLevel;
import net.yadan.banana.DefaultFormatter;
import net.yadan.banana.Formatter;
import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.malloc.MultiSizeAllocator;
import net.yadan.banana.memory.malloc.TreeAllocator;

/**
 * @author omry
 * @created May 4, 2013
 */
public class LinkedList implements ILinkedList {

  public static final int NEXT_OFFSET = 0;
  public static final int DATA_OFFSET = 1;
  public static final int RESERVED_SIZE = DATA_OFFSET;

  private IMemAllocator m_memory;

  private int m_head;
  private int m_tail;
  private int m_size;
  private DebugLevel m_debugLevel;
  private Formatter m_linkFormatter;

  public LinkedList(int maxBlocks, int blockSize, double growthFactor) {
    init(new TreeAllocator(maxBlocks, blockSize + RESERVED_SIZE, growthFactor));
  }

  public LinkedList(int maxBlocks, int sizes[], double growthFactor) {
    int sizes1[] = new int[sizes.length];
    for (int i = 0; i < sizes.length; i++) {
      sizes1[i] = sizes[i] + RESERVED_SIZE;
    }
    init(new MultiSizeAllocator(maxBlocks, sizes1, growthFactor));
  }

  public LinkedList(IMemAllocator memory) {
    init(memory);
  }

  protected void init(IMemAllocator memory) {
    m_memory = memory;
    m_head = -1;
    m_tail = -1;
    m_size = 0;
    m_linkFormatter = new DefaultFormatter();
  }

  @Override
  public int insertHead(int size) {
    int link = m_memory.malloc(size + RESERVED_SIZE);
    m_memory.setInt(link, NEXT_OFFSET, m_head);
    m_head = link;
    if (m_tail == -1) {
      m_tail = link;
    }
    m_size++;
    return link;
  }

  @Override
  public int insert(int size, int anchor) {
    int link = m_memory.malloc(size + RESERVED_SIZE);
    if (m_head == -1 && anchor == m_head) {
      m_head = link;
      m_tail = link;
      m_memory.setInt(link, NEXT_OFFSET, -1);
    } else {
      int next = m_memory.getInt(anchor, NEXT_OFFSET);
      m_memory.setInt(link, NEXT_OFFSET, next);
      m_memory.setInt(anchor, NEXT_OFFSET, link);
      if (anchor == m_tail) {
        m_tail = link;
      }
    }
    m_size++;
    return link;
  }

  @Override
  public void removeHead() {
    if (m_head != -1) {
      int head = m_head;
      m_head = m_memory.getInt(m_head, NEXT_OFFSET);
      m_memory.free(head);
      m_size--;
    }

    if (m_head == -1) {
      m_tail = -1;
    }
  }

  @Override
  public int appendTail(int size) {
    int link = m_memory.malloc(size + RESERVED_SIZE);
    if (m_tail != -1) {
      m_memory.setInt(m_tail, NEXT_OFFSET, link);
    }

    if (m_head == -1) {
      m_head = link;
    }

    m_memory.setInt(link, NEXT_OFFSET, -1);
    m_tail = link;
    m_size++;
    return link;
  }

  @Override
  public void remove(int link) {
    if (link == m_head) {
      m_head = m_memory.getInt(link, NEXT_OFFSET);
    } else {
      int n = m_head;
      while (n != -1) {
        int next = m_memory.getInt(n, NEXT_OFFSET);

        if (next == link) {
          if (next == m_tail) {
            m_tail = n;
          }

          int nextnext = m_memory.getInt(next, NEXT_OFFSET);
          m_memory.setInt(n, NEXT_OFFSET, nextnext);
          break;
        }
        n = next;
      }
    }
    m_size--;

    m_memory.free(link);
  }

  @Override
  public int getHead() {
    return m_head;
  }

  @Override
  public int getTail() {
    return m_tail;
  }

  @Override
  public int getNext(int link) {
    return m_memory.getInt(link, NEXT_OFFSET);
  }

  @Override
  public int getInt(int link, int offset_in_data) {
    return m_memory.getInt(link, offset_in_data + DATA_OFFSET);
  }

  @Override
  public short getUpperShort(int link, int offset) {
    return m_memory.getUpperShort(link, offset + DATA_OFFSET);
  }

  @Override
  public short getLowerShort(int link, int offset) {
    return m_memory.getLowerShort(link, offset + DATA_OFFSET);
  }

  @Override
  public void setUpperShort(int link, int offset, int s) {
    m_memory.setUpperShort(link, offset + DATA_OFFSET, s);
  }

  @Override
  public void setLowerShort(int link, int offset, int s) {
    m_memory.setLowerShort(link, offset + DATA_OFFSET, s);
  }

  @Override
  public void setInt(int link, int offset_in_data, int data) {
    m_memory.setInt(link, offset_in_data + DATA_OFFSET, data);
  }

  @Override
  public void setInts(int link, int dst_offset_in_record, int[] src_data, int src_pos, int length) {
    m_memory.setInts(link, dst_offset_in_record + DATA_OFFSET, src_data, src_pos, length);
  }

  @Override
  public void getInts(int link, int src_offset_in_record, int[] dst_data, int dst_pos, int length) {
    m_memory.getInts(link, src_offset_in_record + DATA_OFFSET, dst_data, dst_pos, length);
  }

  @Override
  public void getBuffer(int link, int src_offset_in_record, IBuffer dst, int length) {
    m_memory.getBuffer(link, DATA_OFFSET + src_offset_in_record, dst, length);
  }

  @Override
  public long getLong(int link, int offset_in_data) {
    return m_memory.getLong(link, offset_in_data + DATA_OFFSET);
  }

  @Override
  public void setLong(int link, int offset_in_data, long data) {
    m_memory.setLong(link, offset_in_data + DATA_OFFSET, data);
  }

  @Override
  public int getPrev(int link) {
    throw new UnsupportedOperationException();
  }

  @Override
  public IMemAllocator getAllocator() {
    return m_memory;
  }

  @Override
  public int size() {
    return m_size;
  }

  @Override
  public int insertHead(IBuffer data) {
    int ret = insertHead(data.size());
    setInts(ret, 0, data.array(), 0, data.size());
    return ret;
  }

  @Override
  public int insert(IBuffer data, int anchor) {
    int ret = insert(data.size(), anchor);
    setInts(ret, 0, data.array(), 0, data.size());
    return ret;
  }

  @Override
  public int appendTail(IBuffer data) {
    int ret = appendTail(data.size());
    setInts(ret, 0, data.array(), 0, data.size());
    return ret;
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public void clear() {
    int n = m_head;
    while (n != -1) {
      int next = m_memory.getInt(n, NEXT_OFFSET);
      m_memory.free(n);
      n = next;
    }

    m_head = m_tail = -1;
    m_size = 0;
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
  public String toString() {
    return ListUtil.listToString(this);
  }

  @Override
  public int maximumCapacityFor(int link) {
    return m_memory.maximumCapacityFor(link) - RESERVED_SIZE;
  }

  @Override
  public void setFormatter(Formatter formatter) {
    m_linkFormatter = formatter;
  }

  @Override
  public Formatter getFormatter() {
    return m_linkFormatter;
  }

  @Override
  public float getFloat(int link, int offset) {
    return m_memory.getFloat(link, DATA_OFFSET + offset);
  }

  @Override
  public void setFloat(int link, int offset, float f) {
    m_memory.setFloat(link, DATA_OFFSET + offset, f);
  }

  @Override
  public double getDouble(int link, int offset_in_data) {
    return m_memory.getDouble(link, offset_in_data + DATA_OFFSET);
  }

  @Override
  public void setDouble(int link, int offset_in_data, double data) {
    m_memory.setDouble(link, offset_in_data + DATA_OFFSET, data);
  }

  @Override
  public void setChars(int pointer, int dst_offset, char[] src_data, int src_pos, int num_chars) {
    m_memory.setChars(pointer, dst_offset + DATA_OFFSET, src_data, src_pos, num_chars);
  }

  @Override
  public void getChars(int pointer, int src_offset, char[] dst_data, int dst_pos, int num_chars) {
    m_memory.getChars(pointer, src_offset + DATA_OFFSET, dst_data, dst_pos, num_chars);
  }
}
