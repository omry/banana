package banana.map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import banana.map.DebugLevel;
import banana.map.HashMap;
import banana.map.IHashMap;
import banana.memory.IMemAllocator;
import banana.memory.initializers.MemSetInitializer;
import banana.memory.malloc.ChainedAllocator;


public class HashMapTest {

  private static final int BLOCK_SIZE = 10;

  protected IHashMap create(int initialCapacity, double loadFactor) {
    IMemAllocator allocator = new ChainedAllocator(100, BLOCK_SIZE, 2.0);
    allocator.setDebug(true);
    allocator.setInitializer(new MemSetInitializer(-1));
    HashMap map = new HashMap(allocator, initialCapacity, loadFactor);
    map.setDebug(DebugLevel.DEBUG_STRUCTURE);
    return map;
  }

  @Test
  public void testHashMapIntFloat() {
    IHashMap h = create(10, 0.75f);
    assertEquals(10, h.getCapacity());
    assertEquals(0.75f, h.getLoadFactor(), Float.MIN_VALUE);
  }

  @Test
  public void testSizeAfterAdd() {
    IHashMap h = create(20, 1.0);

    for (int i = 1; i <= 20; i++) {
      h.createRecord(i, BLOCK_SIZE);
      assertEquals(i, h.size());
    }
  }

  @Test
  public void testSizeAfterRemove() {
    IHashMap h = create(20, 1.0);
    for (int i = 1; i <= 20; i++) {
      h.createRecord(i, BLOCK_SIZE);
    }

    assertEquals(20, h.size());
    for (int i = 1; i <= 20; i++) {
      h.remove(i);
      assertEquals(20 - i, h.size());
    }
  }

  @Test
  public void testPutRecord() {
    IHashMap h = create(10, 0.75f);
    int pointer = h.createRecord(1000, BLOCK_SIZE);
    assertEquals(-1, h.findRecord(100));
    assertEquals(pointer, h.findRecord(1000));
  }

  @Test
  public void testPutRecordWithSameKey() {
    IHashMap h = create(10, 0.75f);
    IMemAllocator mem = h.getAllocator();
    assertEquals(0, mem.usedBlocks());
    int pointer1 = h.createRecord(1000, BLOCK_SIZE);
    assertEquals(pointer1, h.findRecord(1000));
    assertEquals(1, mem.usedBlocks());

    h.setInt(pointer1, 0, 19);
    int pointer2 = h.createRecord(1000, BLOCK_SIZE);
    assertEquals(19, h.getInt(pointer2, 0));

    h.setInt(pointer2, 0, 29);
    assertEquals(pointer2, h.findRecord(1000));
    assertEquals(1, mem.usedBlocks());
    assertEquals(1, h.size());
  }

  @Test
  public void testIsEmpty() {
    IHashMap h = create(10, 0.75f);
    assertTrue(h.isEmpty());
    h.createRecord(10, BLOCK_SIZE);
    assertFalse(h.isEmpty());
  }

  @Test
  public void testContainsKey() {
    IHashMap h = create(10, 0.75f);
    assertFalse(h.containsKey(100));
    h.createRecord(100, BLOCK_SIZE);
    assertTrue(h.containsKey(100));
  }

  @Test
  public void testLongData() {
    IHashMap h = create(10, 0.75f);
    int pointer = h.createRecord(1000, BLOCK_SIZE);
    h.setLong(pointer, 0, Long.MAX_VALUE);
    assertEquals(Long.MAX_VALUE, h.getLong(pointer, 0));

    h.setLong(pointer, 0, 800);
    assertEquals(800, h.getLong(pointer, 0));
  }

  @Test
  public void testIntData() {
    IHashMap h = create(10, 0.75f);
    int pointer = h.createRecord(1000, BLOCK_SIZE);
    h.setInt(pointer, 0, Integer.MAX_VALUE);
    assertEquals(Integer.MAX_VALUE, h.getInt(pointer, 0));

    h.setInt(pointer, 1, 800);
    assertEquals(800, h.getInt(pointer, 1));
  }

  @Test
  public void testRemove() {
    IHashMap h = create(10, 0.75f);
    int pointer = h.createRecord(1000, BLOCK_SIZE);
    assertEquals(pointer, h.findRecord(1000));
    assertEquals(1, h.size());
    h.remove(1000);
    assertEquals(0, h.size());
  }

  @Test
  public void testClear() {
    IHashMap h = create(10, 0.75f);
    assertEquals(0, h.size());
    for (int i = 1; i <= 20; i++) {
      h.createRecord(i, BLOCK_SIZE);
    }

    h.clear();
    assertEquals(0, h.size());
  }

  @Test
  public void testHashKeyOverflow() {
    IHashMap h = create(100000, 0.75f);
    h.createRecord(100003995712499L, BLOCK_SIZE);
  }

  @Test
  public void testGrowth() {
    IHashMap h = create(7, 0.8);
    for (int i = 0; i < 5; i++) {
      int n = h.createRecord(i * i * i + 2, BLOCK_SIZE);
      h.setInt(n, 0, i);
    }

    int n = h.createRecord(5 * 5 * 5 + 2, BLOCK_SIZE);
    h.setInt(n, 0, 5);

    assertEquals(6, h.size());

    for (int i = 0; i < h.size(); i++) {
      assertEquals(i, h.getInt(h.findRecord(i * i * i + 2), 0));
    }
  }

  @Test
  public void testReallocRecord() {
    IHashMap h = create(10, 0.8);
    for (int i = 0; i < 10; i++) {
      int r = h.createRecord(i * i, 1);
//      System.out.println(h.getAllocator().pointerDebugString(r));
      h.setInt(r, 0, i);
    }

    for (int i = 0; i < 10; i++) {
      int r = h.reallocRecord(i * i, 15);
      h.setInt(r, 10, i);
//      System.out.println(h.getAllocator().pointerDebugString(r));
    }
  }
}
