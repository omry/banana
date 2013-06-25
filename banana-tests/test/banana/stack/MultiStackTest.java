package banana.stack;

import static org.junit.Assert.*;

import org.junit.Test;

import banana.memory.Buffer;
import banana.memory.IBuffer;
import banana.stack.MultiStack;
import banana.stack.Stack;


public class MultiStackTest {

  private static final int BLOCK_SIZE = 5;

  @Test
  public void testMultiStack() {
    MultiStack m = new MultiStack(10, 20, BLOCK_SIZE);
    assertEquals(10, m.getNumStacks());
    assertEquals(20, m.availableBlocks());
    assertEquals(BLOCK_SIZE, m.blockSize());
  }

  @Test
  public void testClear() {
    MultiStack m = new MultiStack(10, 20, BLOCK_SIZE);
    m.push(1, BLOCK_SIZE);
    m.push(2, BLOCK_SIZE);
    assertEquals(18, m.availableBlocks());
    m.clear();
    assertEquals(20, m.availableBlocks());
  }

  @Test
  public void testClearInt() {
    MultiStack m = new MultiStack(10, 6, BLOCK_SIZE);
    m.setDebug(true);
    int s1[] = { 1, 2, 3 };
    int s2[] = { 4, 5, 6 };
    // insert into stack 0
    for (int d : s1) {
      m.push(0, BLOCK_SIZE);
      m.setInt(0, d, 0);
    }

    // insert into stack 1
    for (int d : s2) {
      m.push(1, BLOCK_SIZE);
      m.setInt(1, d, 0);
    }

    assertEquals(0, m.availableBlocks());
    // clear stack 1
    m.clear(1);
    assertEquals(3, m.availableBlocks());

    // make sure stack 0 is unchanged
    int index = s1.length;
    while (!m.isEmpty(0)) {
      assertEquals(s1[--index], m.getInt(0, 0));
      m.pop(0);
    }
  }

  @Test
  public void testPushPop() {
    MultiStack m = new MultiStack(2, 2, BLOCK_SIZE);
    m.push(0, BLOCK_SIZE);
    m.setInt(0, 99, 0);
    assertEquals(99, m.getInt(0, 0));

    m.push(0, BLOCK_SIZE);
    m.setInt(0, 98, 0);
    assertEquals(98, m.getInt(0, 0));

    m.pop(0);
    assertEquals(99, m.getInt(0, 0));

    m.pop(0);
    assertTrue(m.isEmpty(0));
  }

  @Test
  public void testSet_int() {
    MultiStack m = new MultiStack(2, 2, BLOCK_SIZE);
    m.push(0, BLOCK_SIZE);
    m.setInt(0, 99, 0);
    assertEquals(99, m.getInt(0, 0));
  }

  @Test
  public void testSet_long() {
    MultiStack m = new MultiStack(2, 2, BLOCK_SIZE);
    m.push(0, BLOCK_SIZE);
    m.setLong(0, Integer.MAX_VALUE * 3L, 0);
    assertEquals(Integer.MAX_VALUE * 3L, m.getLong(0, 0));
  }

  @Test
  public void testSet_ints() {
    int data[] = { 1, 2, 3, 4 };
    int blockSize = data.length;
    MultiStack m = new MultiStack(2, 2, blockSize);
    m.push(0, blockSize);
    m.setInts(0, data, 0, data.length, 0);
    int data2[] = new int[data.length];
    m.getInts(0, 0, data2, 0, data2.length);
    assertArrayEquals(data, data2);
  }

  @Test
  public void testIsEmpty() {
    MultiStack m = new MultiStack(2, 2, BLOCK_SIZE);
    assertTrue(m.isEmpty(0));
    m.push(0, BLOCK_SIZE);
    assertFalse(m.isEmpty(0));
    m.pop(0);
    assertTrue(m.isEmpty(0));
  }

  @Test
  public void testUsedBlocks() {
    MultiStack m = new MultiStack(2, 2, BLOCK_SIZE);
    assertEquals(0, m.usedBlocks());
    m.push(0, BLOCK_SIZE);
    assertEquals(1, m.usedBlocks());
    m.push(0, BLOCK_SIZE);
    assertEquals(2, m.usedBlocks());
  }

  @Test
  public void testMaxBlocks() {
    MultiStack m = new MultiStack(2, 10, BLOCK_SIZE);
    assertEquals(10, m.maxBlock());
  }

  @Test
  public void testIteration() {
    MultiStack m = new MultiStack(2, 10, BLOCK_SIZE);
    m.setDebug(true);
    int s1[] = { 1, 2, 3, 4, 5 };
    for (int d : s1) {
      m.push(0, BLOCK_SIZE);
      m.setInt(0, d, 0);
    }
    int index = s1.length;

    while (!m.isEmpty(0)) {
      assertEquals(s1[--index], m.getInt(0, 0));
      m.pop(0);
    }
  }

  @Test
  public void testEnsureNumStacks() {
    MultiStack m = new MultiStack(1, 2, BLOCK_SIZE);
    m.push(0, BLOCK_SIZE);
    m.setInt(0, 101, 0);
    try {
      m.push(1, BLOCK_SIZE);
      fail("Should have failed push into stack 1");
    } catch (Exception e) {
    }

    assertEquals(101, m.getInt(0, 0));
    m.ensureNumStacks(2);

    assertEquals(2, m.getNumStacks());
    m.push(1, BLOCK_SIZE);
    m.setInt(1, 201, 0);
    assertEquals(101, m.getInt(0, 0));
    assertEquals(201, m.getInt(1, 0));
  }

  @Test
  public void testPushBuffer() {
    Stack s = new Stack(100, BLOCK_SIZE);
    IBuffer buffer = new Buffer(20);
    IBuffer out = new Buffer(20);
    for(int i=0;i<buffer.capacity();i++) {
      buffer.appendInt(i);
    }
    s.push(buffer);
    s.getInts(0, out.array(), 0, buffer.capacity());
    assertArrayEquals(buffer.array(), out.array());
  }
}
