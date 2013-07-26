/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.utils;

import net.yadan.banana.list.DoubleLinkedList;
import net.yadan.banana.map.HashMap;
import net.yadan.banana.map.IHashMap;


/**
 * LRU (Least Recently Used) cache This cache supports various data types, see
 * {@link DataType}
 */
public class LRU {

  /**
   * Specifies what kind of data this LRU should support:
   */
  public enum DataType {
    /**
     * No data : useful to model LRU behavior with minimal overhead
     */
    NONE,

    /**
     * Data is a primitive long
     */
    LONG,

    /**
     * Data is a primitive int
     */
    INT,

    /**
     * Data is any object. this is the most expenive mode
     */
    OBJECT
  }

  private IHashMap m_lruMap;
  private DoubleLinkedList m_lruList;

  private Object m_data[];
  private DoubleLinkedList m_freeList;

  private final int m_maxCapacity;

  private static int LIST_NODE_OFFSET = 0;
  private static int DATA_OFFSET = 1;
  private final DataType m_type;
  private int m_mapRecordSize;
  private int m_listRecordSize;
  private int m_freeListRecordSize;

  /**
   * Constructs a new LRU which the specific capacity and data type
   *
   * @param maxCapacity LRU capacity
   * @param type data type (see {@link DataType})
   */
  public LRU(int maxCapacity, DataType type) {
    m_type = type;
    m_mapRecordSize = recordSize(type);
    m_lruMap = new HashMap(maxCapacity + 1, m_mapRecordSize, 0, 1.0);
    m_lruMap.setGrowthFactor(0);
    m_maxCapacity = maxCapacity;
    m_listRecordSize = 2;

    m_lruList = new DoubleLinkedList(maxCapacity + 1, m_listRecordSize, 0);
    if (m_type == DataType.OBJECT) {
      m_freeListRecordSize = 1;
      m_data = new Object[maxCapacity + 1];
      m_freeList = new DoubleLinkedList(maxCapacity + 1, m_freeListRecordSize, 1);

      for (int i = 0; i < m_data.length; i++) {
        int p = m_freeList.appendTail(1);
        m_freeList.setInt(p, 0, i);
      }
    }
  }

  /**
   * Constructs a new LRU which the specific capacity and data type
   *
   * @param maxCapacity LRU capacity
   * @param supportData is true, data type will be {@link DataType#OBJECT}, else
   *          {@link DataType#NONE}.
   */
  public LRU(int maxCapacity, boolean supportData) {
    this(maxCapacity, supportData ? DataType.OBJECT : DataType.NONE);
  }

  /**
   * Adds an id without data to the LRU
   *
   * @param id
   */
  public void add(long id) {
    add(id, null, null);
  }

  /**
   * Adds an id with Object data to the LRU. LRU must be of type
   * {@link DataType#OBJECT}
   *
   * @param id the id of the item
   * @param data object data
   */
  public void add(long id, Object data) {
    add(id, data, null);
  }

  /**
   * Adds an id with the specified Object data. LRU must be of type
   * {@link DataType#OBJECT}
   *
   * @param id
   * @param data
   * @param callback callback to notify in case this add triggers an eviction
   */
  public void add(long id, Object data, Callback callback) {
    if (data != null)
      assertDataType(DataType.OBJECT);
    add(id, data, 0, 0, callback);
  }

  /**
   * Adds an id with the specified long data. LRU must be of type
   * {@link DataType#LONG}
   *
   * @param id
   * @param data
   */
  public void addLong(long id, long data) {
    addLong(id, data, null);
  }

  /**
   * Adds an id with the specified long data. LRU must be of type
   * {@link DataType#LONG}
   *
   * @param id
   * @param data
   * @param callback callback to notify in case this add triggers an eviction
   */
  public void addLong(long id, long data, Callback callback) {
    assertDataType(DataType.LONG);
    add(id, null, data, 0, callback);
  }

  /**
   * Adds an id with the specified int data. LRU must be of type
   * {@link DataType#INT}
   *
   * @param id
   * @param data
   */
  public void addInt(long id, int data) {
    addInt(id, data, null);
  }

  /**
   * Adds an id with the specified int data. LRU must be of type
   * {@link DataType#INT}
   *
   * @param id
   * @param data
   * @param callback callback to notify in case this add triggers an eviction
   */
  public void addInt(long id, int data, Callback callback) {
    if (m_type != DataType.INT) {
      throw new IllegalArgumentException("int data is not supported for this instance");
    }

    add(id, null, 0, data, callback);
  }

  private void add(long id, Object data, long longdata, int intdata, Callback callback) {
    int node = m_lruMap.findRecord(id);
    if (node != -1) {
      if (m_type == DataType.OBJECT) {
        int index = m_lruMap.getInt(node, DATA_OFFSET);
        m_data[index] = data;
      }

      int item_node = m_lruMap.getInt(node, LIST_NODE_OFFSET);

      m_lruList.remove(item_node);
      item_node = m_lruList.appendTail(m_listRecordSize);
      m_lruList.setLong(item_node, 0, id);
      m_lruMap.setInt(node, LIST_NODE_OFFSET, item_node);

      // gets optimized away when assertions are off
      assert id == m_lruList.getLong(item_node, 0) : "Mismatched value retrieved for " + id;

    } else {
      // first time we are inserting an item with this key.
      int freeIndex = -1;
      if (m_type == DataType.OBJECT) {
        assert m_freeList.size() > 0 : "Ran out of free items in free pool";
        freeIndex = m_freeList.getInt(m_freeList.getHead(), 0);
        m_freeList.removeHead();
        assert m_data[freeIndex] == null : "Got item with data from free pool";
        m_data[freeIndex] = data;
      }

      int newNode = m_lruMap.createRecord(id, m_mapRecordSize);
      int newLRUItem = m_lruList.appendTail(m_listRecordSize);
      m_lruList.setLong(newLRUItem, 0, id);

      m_lruMap.setInt(newNode, LIST_NODE_OFFSET, newLRUItem);
      if (m_type == DataType.OBJECT) {
        m_lruMap.setInt(newNode, DATA_OFFSET, freeIndex);
      } else if (m_type == DataType.INT) {
        m_lruMap.setInt(newNode, DATA_OFFSET, intdata);
      } else if (m_type == DataType.LONG) {
        m_lruMap.setLong(newNode, DATA_OFFSET, longdata);
      }
    }

    while (m_lruList.size() > m_maxCapacity) {
      // remove least recently used
      long idd = m_lruList.getLong(m_lruList.getHead(), 0);
      m_lruList.removeHead();

      int del = m_lruMap.findRecord(idd);
      assert del != -1 : "Item " + idd + " not found in map ";
      long evictedId = idd;
      m_lruMap.remove(idd);
      Object evictedData = null;
      if (m_type == DataType.OBJECT) {
        int dataIndex = m_lruMap.getInt(del, DATA_OFFSET);
        evictedData = m_data[dataIndex];
        int tail = m_freeList.appendTail(m_freeListRecordSize);
        m_freeList.setInt(tail, 0, dataIndex);
        m_data[dataIndex] = null;
      }

      if (callback != null) {
        callback.keyEvicted(evictedId, evictedData);
      }
    }

    assert m_lruMap.size() == m_lruList.size() : "Inconsistent map and list sizes";
  }

  /**
   * Sets the value of an id to the specified int. LRU must be of type
   * {@link DataType#INT}
   *
   * @param id the id of the item
   * @param value int data
   */
  public void setInt(long key, int value) {
    assertDataType(DataType.INT);
    int n = m_lruMap.findRecord(key);
    if (n != -1) {
      m_lruMap.setInt(n, DATA_OFFSET, value);
    }
  }

  /**
   * Sets the long value for the specified id. LRU must be of type
   * {@link DataType#LONG}
   *
   * @param id the id of the item
   * @param value long data
   */
  public void setLong(long key, long value) {
    assertDataType(DataType.LONG);
    int n = m_lruMap.findRecord(key);
    if (n != -1) {
      m_lruMap.setLong(n, DATA_OFFSET, value);
    }
  }

  /**
   * Gets the int associated with the specified id. LRU must be of type
   * {@link DataType#INT}
   *
   * @param id
   * @return
   * @throws IllegalArgumentException if an item with that id was not found
   */
  public int getInt(long id) throws IllegalArgumentException {
    assertDataType(DataType.INT);
    int node = m_lruMap.findRecord(id);
    if (node != -1) {
      return m_lruMap.getInt(node, DATA_OFFSET);
    } else {
      throw new IllegalArgumentException("ID " + id + " not found");
    }
  }

  /**
   * Gets the long associated with the specified id. LRU must be of type
   * {@link DataType#LONG}
   *
   * @param id
   * @return
   * @throws IllegalArgumentException if an item with that id was not found
   */
  public long getLong(long id) throws IllegalArgumentException {
    assertDataType(DataType.LONG);
    int node = m_lruMap.findRecord(id);
    if (node != -1) {
      return m_lruMap.getLong(node, DATA_OFFSET);
    } else {
      throw new IllegalArgumentException("ID " + id + " not found");
    }
  }

  /**
   * Gets the Object associated with the specified id. LRU must be of type
   * {@link DataType#OBJECT}
   *
   * @param id
   * @return the object or null if not found
   */
  public Object get(long id) {
    assertDataType(DataType.OBJECT);
    int node = m_lruMap.findRecord(id);
    if (node != -1) {
      int index = m_lruMap.getInt(node, DATA_OFFSET);
      return m_data[index];
    } else
      return null;
  }

  /**
   * @return number of items in the LRU
   */
  public long size() {
    return m_lruMap.size();
  }

  /**
   * @param id
   * @return true if the id exists in the LRU
   */
  public boolean exists(long id) {
    return m_lruMap.containsKey(id);
  }

  /**
   * Computes the approximate size in bytes an LRU with the specified size and
   * type will consume
   *
   * @param maxCapacity capacity
   * @param type type
   * @return size in bytes
   */
  public static long computerMemoryUsage(int maxCapacity, DataType type) {
    long ret = 4 * HashMap.getIntArraySize(maxCapacity + 1, recordSize(type));
    if (type == DataType.OBJECT) {
      ret += 8 * (maxCapacity + 1); // estimate for object pool array
      ret += 4 * DoubleLinkedList.getIntArraySize(maxCapacity + 1, 1); // for
                                                                     // freelist
    }

    return ret;
  }

  @Override
  public String toString() {
    String data = "";
    switch (m_type) {
    case NONE:
      break;
    case OBJECT:
    data = " with Object data support";
      break;
    case LONG:
    data = " with long data support";
      break;
    case INT:
    data = " with int data support";
      break;
    }
    return "LRU : " + size() + "/" + m_maxCapacity + data;
  }

  public int capacity() {
    return m_maxCapacity;
  }

  public interface Callback {
    public void keyEvicted(long key, Object data);
  }

  private static int recordSize(DataType type) {
    int recordSize = 1;
    int dataSize = 0;
    switch (type) {
    case NONE:
    dataSize = 0;
      break;
    case LONG:
    dataSize = 2;
      break;
    case INT:
    case OBJECT:
    dataSize = 1;
      break;
    }
    recordSize += dataSize;
    return recordSize;
  }

  private void assertDataType(DataType type) {
    if (m_type != type)
      throw new UnsupportedOperationException("Data type is not " + type);
  }
}
