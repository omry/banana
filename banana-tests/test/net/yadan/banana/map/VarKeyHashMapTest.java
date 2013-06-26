package net.yadan.banana.map;

import net.yadan.banana.memory.Buffer;
import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.OutOfMemoryException;
import net.yadan.banana.memory.initializers.MemSetInitializer;
import net.yadan.banana.memory.malloc.ChainedAllocator;
import org.junit.Test;

import static org.junit.Assert.*;


public class VarKeyHashMapTest {

  private static final String PREFIX = "hello ";
  private static final int BLOCK_SIZE = 10;

  protected IVarKeyHashMap create(int initialCapacity, double loadFactor) {
    IMemAllocator valuesAllocator = new ChainedAllocator(100, VarKeyHashMap.RESERVED_SIZE
        + BLOCK_SIZE, 2.0);
    IMemAllocator keysAllocator = new ChainedAllocator(100, BLOCK_SIZE, 2.0);
    valuesAllocator.setDebug(true);
    valuesAllocator.setInitializer(new MemSetInitializer(-1));
    keysAllocator.setDebug(true);
    keysAllocator.setInitializer(new MemSetInitializer(-1));

    IVarKeyHashMap map = new VarKeyHashMap(valuesAllocator, keysAllocator, initialCapacity,
        loadFactor);
    map.setDebug(DebugLevel.DEBUG_STRUCTURE);
    return map;
  }

  @Test
  public void testHashMapIntFloat() {
    IVarKeyHashMap h = create(10, 0.75f);
    assertEquals(10, h.getCapacity());
    assertEquals(0.75f, h.getLoadFactor(), Float.MIN_VALUE);
  }

  @Test
  public void testSizeAfterAdd() {
    IVarKeyHashMap h = create(20, 1.0);
    IBuffer key = new Buffer(10);

    for (int i = 1; i <= 20; i++) {
      key.appendChars((PREFIX + i).toCharArray());
      h.createRecord(key, BLOCK_SIZE);
      assertEquals(i, h.size());
      key.reset();
    }
  }

  @Test
  public void testSizeAfterRemove() {
    IBuffer key = new Buffer(10);
    IVarKeyHashMap h = create(20, 1.0);
    for (int i = 1; i <= 20; i++) {
      key.appendChars((PREFIX + i).toCharArray());
      h.createRecord(key, BLOCK_SIZE);
      key.reset();
    }

    assertEquals(20, h.size());
    for (int i = 1; i <= 20; i++) {
      key.appendChars((PREFIX + i).toCharArray());
      h.remove(key);
      assertEquals(20 - i, h.size());
      key.reset();
    }
  }

  @Test
  public void testPutRecord() {
    IBuffer key = new Buffer(10);
    IBuffer key2 = new Buffer(10);
    IVarKeyHashMap h = create(10, 0.75f);
    key.appendChars((PREFIX + 1000).toCharArray());
    int pointer = h.createRecord(key, BLOCK_SIZE);

    key2.appendChars((PREFIX + 100).toCharArray());
    assertEquals(-1, h.findRecord(key2));
    assertEquals(pointer, h.findRecord(key));
  }

  @Test
  public void testPutRecordWithSameKey() {
    IVarKeyHashMap h = create(10, 0.75f);

    IMemAllocator keys = h.keysMemory();
    IMemAllocator values = h.valueMemory();
    assertEquals(0, keys.usedBlocks());
    assertEquals(0, values.usedBlocks());

    IBuffer key = new Buffer(10);
    key.appendChars((PREFIX + 1000).toCharArray());
    int pointer1 = h.createRecord(key, BLOCK_SIZE);
    assertEquals(pointer1, h.findRecord(key));
    assertEquals(1, keys.usedBlocks());
    assertEquals(1, values.usedBlocks());

    h.setInt(pointer1, 0, 19);
    int pointer2 = h.createRecord(key, BLOCK_SIZE);
    assertEquals(19, h.getInt(pointer2, 0));
    assertEquals(1, keys.usedBlocks());
    assertEquals(1, values.usedBlocks());

    h.setInt(pointer2, 0, 29);
    assertEquals(pointer2, h.findRecord(key));
    assertEquals(1, keys.usedBlocks());
    assertEquals(1, h.size());

  }

  @Test
  public void testIsEmpty() {
    IBuffer key = new Buffer(10);
    key.appendChars((PREFIX + 10).toCharArray());

    IVarKeyHashMap h = create(10, 0.75f);
    assertTrue(h.isEmpty());
    h.createRecord(key, BLOCK_SIZE);
    assertFalse(h.isEmpty());
  }

  @Test
  public void testContainsKey() {
    IBuffer key = new Buffer(10);
    key.appendChars((PREFIX + 100).toCharArray());
    IVarKeyHashMap h = create(10, 0.75f);
    assertFalse(h.containsKey(key));
    h.createRecord(key, BLOCK_SIZE);
    assertTrue(h.containsKey(key));
  }

  @Test
  public void testLongData() {
    IVarKeyHashMap h = create(10, 0.75f);
    IBuffer key = new Buffer(10);
    key.appendChars((PREFIX + 1000).toCharArray());
    int pointer = h.createRecord(key, BLOCK_SIZE);
    h.setLong(pointer, 0, Long.MAX_VALUE);
    assertEquals(Long.MAX_VALUE, h.getLong(pointer, 0));

    h.setLong(pointer, 0, 800);
    assertEquals(800, h.getLong(pointer, 0));
  }

  @Test
  public void testIntData() {
    IVarKeyHashMap h = create(10, 0.75f);
    IBuffer key = new Buffer(10);
    key.appendChars((PREFIX + 1000).toCharArray());
    int pointer = h.createRecord(key, BLOCK_SIZE);
    h.setInt(pointer, 0, Integer.MAX_VALUE);
    assertEquals(Integer.MAX_VALUE, h.getInt(pointer, 0));

    h.setInt(pointer, 1, 800);
    assertEquals(800, h.getInt(pointer, 1));
  }

  @Test
  public void testRemove() {
    IVarKeyHashMap h = create(10, 0.75f);
    IBuffer key = new Buffer(10);
    key.appendChars((PREFIX + 1000).toCharArray());
    int pointer = h.createRecord(key, BLOCK_SIZE);
    assertEquals(pointer, h.findRecord(key));
    assertEquals(1, h.size());
    h.remove(key);
    assertEquals(0, h.size());
  }

  @Test
  public void testClear() {
    IVarKeyHashMap h = create(10, 0.75f);
    IBuffer key = new Buffer(10);
    assertEquals(0, h.size());
    for (int i = 1; i <= 20; i++) {
      key.appendChars((PREFIX + i).toCharArray());
      h.createRecord(key, BLOCK_SIZE);
      key.reset();
    }

    h.clear();
    assertEquals(0, h.size());
  }

  @Test
  public void testHashKeyOverflow() {
    IVarKeyHashMap h = create(100000, 0.75f);
    IBuffer key = new Buffer(20);
    key.appendChars("1234567890123456789012345678901234567890".toCharArray());
    h.createRecord(key, BLOCK_SIZE);
  }

  @Test
  public void testGrowth() {
    IVarKeyHashMap h = create(10, 0.5);
    IBuffer key = new Buffer(10);
    for (int i = 0; i < 5; i++) {
      key.appendChars((PREFIX + (i * i * i)).toCharArray());
      int n = h.createRecord(key, BLOCK_SIZE);
      h.setInt(n, 0, i);
      key.reset();
    }

    key.appendChars((PREFIX + (5 * 5 * 5)).toCharArray());
    int n = h.createRecord(key, BLOCK_SIZE);
    h.setInt(n, 0, 5);
    key.reset();

    assertEquals(6, h.size());

    for (int i = 0; i < h.size(); i++) {
      key.appendChars((PREFIX + (i * i * i)).toCharArray());
      assertEquals(i, h.getInt(h.findRecord(key), 0));
      key.reset();
    }
  }

  @Test
  public void testOutOfMemory() {

    IMemAllocator valuesAllocator = new ChainedAllocator(5, VarKeyHashMap.RESERVED_SIZE
        + BLOCK_SIZE, 0);
    IMemAllocator keysAllocator = new ChainedAllocator(5, BLOCK_SIZE, 0);
    valuesAllocator.setDebug(true);
    valuesAllocator.setInitializer(new MemSetInitializer(-1));
    keysAllocator.setDebug(true);
    keysAllocator.setInitializer(new MemSetInitializer(-1));

    IVarKeyHashMap h = new VarKeyHashMap(keysAllocator, valuesAllocator, 10, 1);

    IBuffer key = new Buffer(5 * BLOCK_SIZE);
    for (int i = 0; i < key.capacity(); i++) {
      key.appendInt(i);
    }

    IBuffer value = new Buffer(5 * BLOCK_SIZE);
    value.setUsed(5 * BLOCK_SIZE);

    try {
      h.createRecord(key, value);
      fail();
    } catch (OutOfMemoryException e) {
    }

    assertEquals(0, valuesAllocator.usedBlocks());
    assertEquals(0, keysAllocator.usedBlocks());
  }

  @Test
  public void testReallocRecord() {
    IBuffer key = new Buffer(5 * BLOCK_SIZE);
    IVarKeyHashMap h = create(10, 0.8);
    for (int i = 0; i < 10; i++) {
      key.appendInt(i * i);
      int r = h.createRecord(key, 1);
//      System.out.println(h.valueMemory().pointerDebugString(r));
      h.setInt(r, 0, i);
      key.reset();
    }

    for (int i = 0; i < 10; i++) {
      key.appendInt(i * i);
      int r = h.reallocRecord(key, 15);
      h.setInt(r, 10, i);
//      System.out.println(h.valueMemory().pointerDebugString(r));
      key.reset();
    }
  }
}
