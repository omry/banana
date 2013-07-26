package net.yadan.banana.utils;

import net.yadan.banana.utils.LRU;

import org.junit.Test;

import static org.junit.Assert.*;

public class LRUTest {

  @Test
  public void test1SizedLRU() {
    LRU lru = new LRU(1, true);
    addAndExpectNoEviction(lru, 1);
    assertEquals(1, lru.size());
    assertTrue(lru.exists(1));

    addAndExpectEviction(lru, 2, 1);
    assertEquals(1, lru.size());
    assertFalse(lru.exists(1));
    assertTrue(lru.exists(2));
  }

  @Test
  public void simpleInsertTest() {
    LRU lru = new LRU(5, true);
    for (int i = 0; i < 5; i++) {
      addAndExpectNoEviction(lru, 100 + i);
    }

    for (int i = 5; i < 10; i++) {
      addAndExpectEviction(lru, 100 + i, 100 + i - 5);
    }

    // should end up with 5-9
    for (int i = 5; i < 10; i++) {
      assertTrue(lru.exists(100 + i));
    }
  }

  @Test
  public void testDuplicateAdds() {
    LRU lru = new LRU(3, true);
    lru.add(1);
    lru.add(1);
    assertEquals(1, lru.size());
    assertTrue(lru.exists(1));
  }

  @Test
  public void testLRUTouch() {
    LRU lru = new LRU(3, true);
    lru.add(1);
    lru.add(2);
    lru.add(3);
    assertEquals(3, lru.size());
    assertTrue(lru.exists(1));
    assertTrue(lru.exists(2));
    assertTrue(lru.exists(3));

    // touch 1, making it the newest item
    lru.add(1);
    assertEquals(3, lru.size());

    addAndExpectEviction(lru, 4, 2);
    assertTrue(lru.exists(1));
    assertFalse(lru.exists(2));
    assertTrue(lru.exists(3));
    assertTrue(lru.exists(4));
    assertEquals(3, lru.size());
  }

  @Test
  public void dataInsertTest() {
    LRU lru = new LRU(3, true);
    lru.add(1, "one");
    lru.add(2, "two");
    assertEquals(2, lru.size());
    assertTrue(lru.exists(1));
    assertEquals("one", lru.get(1));
    assertEquals("two", lru.get(2));

  }

  static class EvictExpector implements LRU.Callback {

    private final long m_expectedKey;
    boolean m_evicted = false;

    public EvictExpector() {
      m_expectedKey = -1;
    }

    public EvictExpector(long expectedKey) {
      m_expectedKey = expectedKey;
    }

    @Override
    public void keyEvicted(long key, Object data) {
      if (m_expectedKey != -1)
        assertEquals("Unexpected key evicted", m_expectedKey, key);
      m_evicted = true;
    }
  }

  public void addAndExpectNoEviction(LRU lru, long add) {
    EvictExpector callback = new EvictExpector();
    lru.add(add, null, callback);
    assertTrue("An item was evicted ", !callback.m_evicted);
  }

  public void addAndExpectEviction(LRU lru, long add, long expectedEviction) {
    EvictExpector callback = new EvictExpector(expectedEviction);
    lru.add(add, null, callback);
    assertTrue("No item evicted", callback.m_evicted);
  }

  @Test
  public void testPrimitiveIntData() {
    LRU lru = new LRU(3, LRU.DataType.INT);
    lru.addInt(1, 10);
    lru.addInt(2, 20);
    assertEquals(2, lru.size());
    assertTrue(lru.exists(1));
    assertTrue(lru.exists(2));
    assertEquals(10, lru.getInt(1));
    assertEquals(20, lru.getInt(2));
  }

  @Test
  public void testPrimitiveLongData() {
    LRU lru = new LRU(3, LRU.DataType.LONG);
    lru.addLong(1, 10L * Integer.MAX_VALUE);
    lru.addLong(2, 20L * Integer.MAX_VALUE);
    assertEquals(2, lru.size());
    assertTrue(lru.exists(1));
    assertTrue(lru.exists(2));
    assertEquals(10L * Integer.MAX_VALUE, lru.getLong(1));
    assertEquals(20L * Integer.MAX_VALUE, lru.getLong(2));
    lru.setLong(1, 11);
    lru.setLong(2, 22);
    assertEquals(11, lru.getLong(1));
    assertEquals(22, lru.getLong(2));
  }

}
