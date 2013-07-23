/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.map;

import net.yadan.banana.DebugLevel;
import net.yadan.banana.DefaultFormatter;
import net.yadan.banana.Formatter;
import net.yadan.banana.memory.Buffer;
import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.OutOfMemoryException;
import net.yadan.banana.memory.block.BlockAllocator;
import net.yadan.banana.memory.malloc.MultiSizeAllocator;
import net.yadan.banana.memory.malloc.TreeAllocator;


/**
 * @author omry
 * created May 7, 2013
 */
public class VarKeyHashMap implements IVarKeyHashMap {

  private static final double DEFAULT_GROWTH_FACTOR = 2.0;

  private static final int NEXT_OFFSET = 0;
  private static final int KEY_OFFSET = 1;
  private static final int USER_DATA_OFFSET = 2;
  public static final int RESERVED_SIZE = USER_DATA_OFFSET;

  private static final int KEY_SIZE_OFFSET = 0;
  private static final int KEY_DATA_OFFSET = 1;

  private final double m_loadFactor;
  private double m_growthFactor;

  /**
   * Holds an array of pointers into m_memory
   */
  private int m_table[];

  private int m_size;

  /**
   * The table is rehashed when its size exceeds this threshold. (The value of
   * this field is (int)(capacity * loadFactor).)
   */
  private int m_threshold;

  private DebugLevel m_debugLevel = DebugLevel.NONE;

  private IMemAllocator m_valuesMemory;
  private IMemAllocator m_keysMemory;

  private Formatter m_formatter;

  public VarKeyHashMap(int maxBlocks, int blockSize, double growthFactor, double loadFactor) {
    this(new TreeAllocator(maxBlocks, blockSize + USER_DATA_OFFSET, growthFactor),
        new MultiSizeAllocator(100, new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 25, 50, 100 }, 2.0),
        maxBlocks, loadFactor);
  }

  public VarKeyHashMap(IMemAllocator memory, IMemAllocator keysMemory, int initialCapacity,
      double loadFactor) {
    m_size = 0;
    m_loadFactor = loadFactor;
    m_growthFactor = DEFAULT_GROWTH_FACTOR;
    m_valuesMemory = memory;
    m_table = new int[initialCapacity];
    m_threshold = (int) Math.min(getCapacity() * getLoadFactor(), Integer.MAX_VALUE);

    m_keysMemory = keysMemory;

    m_size = 0;
    for (int i = 0; i < m_table.length; i++) {
      m_table[i] = -1;
    }

    m_formatter = new DefaultFormatter();
  }

  @Override
  public int size() {
    return m_size;
  }

  @Override
  public int getCapacity() {
    return m_table.length;
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean containsKey(IBuffer key) {
    return findRecord(key) != -1;
  }

  @Override
  public int createRecord(IBuffer key, int size) {
    if (size() >= m_threshold && m_growthFactor > 0) {
      increaseCapacity();
    }

    int listNum = hashCode(key, m_table.length);
    int pointer = m_table[listNum];

    // find if this key is already in the chain
    int prev = -1;
    while (pointer != -1) {
      int keyPtr2 = m_valuesMemory.getInt(pointer, KEY_OFFSET);
      int keySize2 = m_keysMemory.getInt(keyPtr2, KEY_SIZE_OFFSET);

      // list already contain this key, reuse the space - resizing as needed
      if (key.equals(m_keysMemory, keyPtr2, KEY_DATA_OFFSET, keySize2)) {
        int pNext = m_valuesMemory.getInt(pointer, NEXT_OFFSET);
        pointer = m_valuesMemory.realloc(pointer, size);
        if (prev == -1) {
          m_table[listNum] = pointer;
        } else {
          m_valuesMemory.setInt(prev, NEXT_OFFSET, pointer);
        }
        m_valuesMemory.initialize(pointer);
        m_valuesMemory.setInt(pointer, KEY_OFFSET, keyPtr2);
        m_valuesMemory.setInt(pointer, NEXT_OFFSET, pNext);
        break;
      }
      prev = pointer;
      pointer = m_valuesMemory.getInt(pointer, NEXT_OFFSET);
    }

    if (pointer == -1) {
      int keySize = key.size();
      int keyPtr = m_keysMemory.malloc(keySize + 1);
      m_keysMemory.setInt(keyPtr, KEY_SIZE_OFFSET, keySize);
      m_keysMemory.setInts(keyPtr, KEY_DATA_OFFSET, key.array(), 0, keySize);
      try {
        pointer = m_valuesMemory.malloc(size + RESERVED_SIZE);
      } catch (OutOfMemoryException e) {
        m_keysMemory.free(keyPtr);
        throw e;
      }
      m_valuesMemory.setInt(pointer, KEY_OFFSET, keyPtr);
      m_valuesMemory.setInt(pointer, NEXT_OFFSET, m_table[listNum]);
      m_table[listNum] = pointer;
      m_size++;
    }

    return pointer;
  }

  @Override
  public int createRecord(IBuffer key, IBuffer value) {
    int ret = createRecord(key, value.size());
    setInts(ret, 0, value.array(), 0, value.size());
    return ret;
  }

  @Override
  public int findRecord(IBuffer key) {
    int listNum = hashCode(key, m_table.length);

    int n = m_table[listNum];
    while (n != -1) {
      int keyPtr = m_valuesMemory.getInt(n, KEY_OFFSET);
      int keySize = m_keysMemory.getInt(keyPtr, KEY_SIZE_OFFSET);
      if (key.equals(m_keysMemory, keyPtr, KEY_DATA_OFFSET, keySize)) {
        break;
      }
      n = m_valuesMemory.getInt(n, NEXT_OFFSET);
    }

    return n;
  }

  @Override
  public int reallocRecord(IBuffer key, int newSize) {
    int listNum = hashCode(key, m_table.length);
    int n = m_table[listNum];
    int prev = -1;
    while (n != -1) {
      int keyPtr = m_valuesMemory.getInt(n, KEY_OFFSET);
      int keySize = m_keysMemory.getInt(keyPtr, KEY_SIZE_OFFSET);
      if (key.equals(m_keysMemory, keyPtr, KEY_DATA_OFFSET, keySize)) {
        int ptr = m_valuesMemory.realloc(n, newSize + RESERVED_SIZE);
        if (n == m_table[listNum]) {
          m_table[listNum] = ptr;
        } else {
          m_valuesMemory.setInt(prev, NEXT_OFFSET, ptr);
        }
        return ptr;
      }
      prev = n;
      n = m_valuesMemory.getInt(n, NEXT_OFFSET);
    }

    return -1;
  }

  @Override
  public void setLong(int record_id, int offset_in_data, long data) {
    m_valuesMemory.setLong(record_id, offset_in_data + USER_DATA_OFFSET, data);
  }

  @Override
  public short getUpperShort(int link, int offset) {
    return m_valuesMemory.getUpperShort(link, offset + USER_DATA_OFFSET);
  }

  @Override
  public short getLowerShort(int link, int offset) {
    return m_valuesMemory.getLowerShort(link, offset + USER_DATA_OFFSET);
  }

  @Override
  public void setUpperShort(int link, int offset, int s) {
    m_valuesMemory.setUpperShort(link, offset + USER_DATA_OFFSET, s);
  }

  @Override
  public void setLowerShort(int link, int offset, int s) {
    m_valuesMemory.setLowerShort(link, offset + USER_DATA_OFFSET, s);
  }

  @Override
  public void setInt(int record_id, int offset_in_data, int data) {
    m_valuesMemory.setInt(record_id, offset_in_data + USER_DATA_OFFSET, data);
  }

  @Override
  public void setFloat(int record_id, int offset, float f) {
    m_valuesMemory.setFloat(record_id, offset + USER_DATA_OFFSET, f);
  }

  @Override
  public float getFloat(int record_id, int offset) {
    return m_valuesMemory.getFloat(record_id, offset + USER_DATA_OFFSET);
  }

  @Override
  public double getDouble(int record_id, int offset_in_data) {
    return m_valuesMemory.getDouble(record_id, offset_in_data + USER_DATA_OFFSET);
  }

  @Override
  public void setDouble(int record_id, int offset_in_data, double data) {
    m_valuesMemory.setDouble(record_id, offset_in_data + USER_DATA_OFFSET, data);
  }

  @Override
  public void setChars(int record_id, int dst_offset, char[] src_data, int src_pos, int num_chars) {
    m_valuesMemory.setChars(record_id, dst_offset + USER_DATA_OFFSET, src_data, src_pos, num_chars);
  }

  @Override
  public void getChars(int record_id, int src_offset, char[] dst_data, int dst_pos, int num_chars) {
    m_valuesMemory.getChars(record_id, src_offset + USER_DATA_OFFSET, dst_data, dst_pos, num_chars);
  }

  @Override
  public long getLong(int record_id, int offset_in_data) {
    return m_valuesMemory.getLong(record_id, offset_in_data + USER_DATA_OFFSET);
  }

  @Override
  public int getInt(int record_id, int offset_in_data) {
    return m_valuesMemory.getInt(record_id, offset_in_data + USER_DATA_OFFSET);
  }

  @Override
  public void setInts(int pointer, int dst_offset_in_record,
      int[] src_data, int src_pos, int length) {
    m_valuesMemory.setInts(pointer, dst_offset_in_record + USER_DATA_OFFSET, src_data, src_pos,
        length);
  }

  @Override
  public void getInts(int pointer, int src_offset_in_record,
      int[] dst_data, int dst_pos, int length) {
    m_valuesMemory.getInts(pointer, src_offset_in_record + USER_DATA_OFFSET, dst_data, dst_pos,
        length);
  }

  @Override
  public void getBuffer(int pointer, int src_offset_in_record, IBuffer dst, int length) {
    m_valuesMemory.getBuffer(pointer, USER_DATA_OFFSET + src_offset_in_record, dst, length);
  }


  @Override
  public boolean remove(IBuffer key) {

    int listNum = hashCode(key, m_table.length);

    int n = m_table[listNum];
    int prev = -1;
    boolean first = true;
    while (n != -1) {
      int keyPtr = m_valuesMemory.getInt(n, KEY_OFFSET);
      int keySize = m_keysMemory.getInt(keyPtr, KEY_SIZE_OFFSET);
      if (key.equals(m_keysMemory, keyPtr, KEY_DATA_OFFSET, keySize)) {
        int next = m_valuesMemory.getInt(n, NEXT_OFFSET);
        if (first) {
          m_table[listNum] = next;
        } else {
          m_valuesMemory.setInt(prev, NEXT_OFFSET, next);
        }
        m_size--;
        m_valuesMemory.free(n);
        m_keysMemory.free(keyPtr);
        return true;
      }
      prev = n;
      first = false;
      n = m_valuesMemory.getInt(n, NEXT_OFFSET);
    }

    return false;
  }

  @Override
  public void clear() {
    m_size = 0;
    visitRecords(new VarKeyHashMapVisitorAdapter() {
      @Override
      public void visit(IVarKeyHashMap map, int keyPtr, int recordPtr, long num, long total) {
        m_valuesMemory.free(recordPtr);
        m_keysMemory.free(keyPtr);
      }
    });
    for (int i = 0; i < m_table.length; i++) {
      m_table[i] = -1;
    }
  }

  @Override
  public double getLoadFactor() {
    return m_loadFactor;
  }

  @Override
  public void setGrowthFactor(double d) {
    if (!(d == 0 || d > 1))
      throw new IllegalArgumentException("Growth factor " + d + " should be > 1 or 0 to disable");
    m_growthFactor = d;
  }

  @Override
  public void visitRecords(VarKeyHashMapVisitor visitor) {
    visitor.begin(this);

    int num = 0;
    long total = size();
    for (int i = 0; i < m_table.length; i++) {
      int n = m_table[i];
      while (n != -1) {
        int keyPtr = m_valuesMemory.getInt(n, KEY_OFFSET);
        int next = m_valuesMemory.getInt(n, NEXT_OFFSET);
        visitor.visit(this, keyPtr, n, num, total);
        n = next;
      }
    }
    visitor.end(this);
  }

  private void increaseCapacity() {
    // long t = Sysftem.currentTimeMillis();
    IBuffer buffer = new Buffer(10, 2);
    int capacity = getCapacity();
    long newCapacity = Math.max(capacity + 1, (long) (capacity * m_growthFactor));
    if (newCapacity > Integer.MAX_VALUE) {
      throw new IllegalStateException("Attempted to resize list to " + newCapacity
          + " which is greated than Integer.MAX_VALUE");
    }
    int intCap = (int) newCapacity;
    int newTable[] = new int[intCap];
    for (int i = 0; i < newTable.length; i++) {
      newTable[i] = -1;
    }

    for (int tableNum = 0; tableNum < m_table.length; tableNum++) {
      int n = m_table[tableNum];
      while (n != -1) {
        int keyPtr = m_valuesMemory.getInt(n, KEY_OFFSET);
        int keySize = m_keysMemory.getInt(keyPtr, KEY_SIZE_OFFSET);
        buffer.ensureCapacity(keySize);
        m_keysMemory.getInts(keyPtr, KEY_DATA_OFFSET, buffer.array(), 0, keySize);
        buffer.setUsed(keySize);
        int newTableNum = hashCode(buffer, intCap);
        buffer.reset();

        int next = m_valuesMemory.getInt(n, NEXT_OFFSET);
        m_valuesMemory.setInt(n, NEXT_OFFSET, newTable[newTableNum]);
        newTable[newTableNum] = n;
        n = next;
      }
    }

    m_table = newTable;
    m_threshold = (int) Math.min(getCapacity() * getLoadFactor(), Integer.MAX_VALUE);
    // System.out.println(String.format("Increased map capacity from %d to %d took %d ms",
    // capacity,
    // intCap, (System.currentTimeMillis() - t)));
  }

  private int hashCode(IBuffer key, int listSize) {
    int h = key.hashCode();
    int r = h % listSize;
    return r < 0 ? r + listSize : r;
  }

  public static int getIntArraySize(int maxCapacity, int recordSize) {
    return BlockAllocator.getIntArraySize(maxCapacity, recordSize + USER_DATA_OFFSET);
  }

  @Override
  public long computeMemoryUsage() {
    return 4 * m_table.length + m_keysMemory.computeMemoryUsage()
        + m_valuesMemory.computeMemoryUsage();
  }

  @Override
  public void setDebug(DebugLevel level) {
    m_debugLevel = level;
    m_keysMemory.setDebug(level != DebugLevel.NONE);
    m_valuesMemory.setDebug(level != DebugLevel.NONE);
  }

  @Override
  public String toString() {
    final StringBuilder s = new StringBuilder();
    s.append(VarKeyHashMap.class.getName()).append(" ").append(size()).append(" / ")
        .append(getCapacity());

    if (m_debugLevel == DebugLevel.DEBUG_CONTENT) {
      s.append("\n");
      visitRecords(new VarKeyHashMapVisitor() {
        @Override
        public void visit(IVarKeyHashMap map, int keyPtr, int valuePtr, long num, long total) {
          IMemAllocator keys = map.keysMemory();
          int keySize = keys.getInt(keyPtr, KEY_SIZE_OFFSET);
          for (int i = KEY_DATA_OFFSET; i < KEY_DATA_OFFSET + keySize; i++) {
            int ii = keys.getInt(keyPtr, i);

            char c1 = (char) (ii >> 16);
            char c2 = (char) (0x00FF & ii);
            if (c1 != 0) {
              s.append(c1);
            }
            if (c2 != 0) {
              s.append(c2);
            }
          }
          s.append("=").append(m_formatter.format(map, valuePtr));
        }

        @Override
        public void end(IVarKeyHashMap map) {
        }

        @Override
        public void begin(IVarKeyHashMap map) {
        }
      });
    } else if (m_debugLevel == DebugLevel.DEBUG_STRUCTURE) {
      s.append("\n");
      for (int tableNum = 0; tableNum < m_table.length; tableNum++) {
        int n = m_table[tableNum];
        s.append(tableNum).append(" : ");
        while (n != -1) {
          IMemAllocator keys = keysMemory();

          int keyPtr = m_valuesMemory.getInt(n, KEY_OFFSET);
          int keySize = keys.getInt(keyPtr, 0);

          int next = m_valuesMemory.getInt(n, NEXT_OFFSET);

          s.append("(#").append(n).append(",K=");
          for (int i = 0; i < keySize; i++) {
            int ii = keys.getInt(keyPtr, i + 1);

            char c1 = (char) (ii >> 16);
            char c2 = (char) (0x00FF & ii);
            if (c1 != 0) {
              s.append(c1);
            }
            if (c2 != 0) {
              s.append(c2);
            }
          }
          s.append(",N=").append(m_valuesMemory.getInt(n, NEXT_OFFSET));
          s.append(")=").append(m_formatter.format(this, n));
          n = next;
          s.append(" -> ");
        }

        s.append("END\n");
      }
    }

    return s.toString();
  }

  @Override
  public IMemAllocator valueMemory() {
    return m_valuesMemory;
  }

  @Override
  public IMemAllocator keysMemory() {
    return m_keysMemory;
  }

  @Override
  public int maximumCapacityFor(int link) {
    return m_valuesMemory.maximumCapacityFor(link) - RESERVED_SIZE;
  }

  @Override
  public void setFormatter(Formatter formatter) {
    m_formatter = formatter;
  }

  @Override
  public DebugLevel getDebug() {
    return m_debugLevel;
  }

  @Override
  public Formatter getFormatter() {
    return m_formatter;
  }

}
