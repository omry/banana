package net.yadan.banana.stack;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import net.yadan.banana.memory.Buffer;
import net.yadan.banana.memory.IBuffer;

import org.junit.Test;

public class StackTest {

  private static final int BLOCK_SIZE = 11;

  @Test
  public void testStack() {
    Stack s = new Stack(10, BLOCK_SIZE);
    assertEquals(10, s.maxBlocks());
    assertEquals(11, s.blockSize());
  }

  @Test
  public void testPush() {
    Stack s = new Stack(2, BLOCK_SIZE);
    assertEquals(0, s.usedBlocks());
    s.push(BLOCK_SIZE);
    assertEquals(1, s.usedBlocks());
    s.push(BLOCK_SIZE);
    assertEquals(2, s.usedBlocks());

    try {
      s.push(BLOCK_SIZE);
      fail("pushed more than 2");
    } catch (Exception e) {
    }

  }

  @Test
  public void testPop() {
    Stack s = new Stack(2, BLOCK_SIZE);
    assertEquals(0, s.usedBlocks());
    try {
      s.pop();
      fail("poped from an empty stack");
    } catch (Exception e) {
    }

    s.push(BLOCK_SIZE);
    assertEquals(1, s.usedBlocks());
    s.pop();
    assertEquals(0, s.usedBlocks());
  }

  @Test
  public void testSet_int() {
    Stack s = new Stack(2, BLOCK_SIZE);
    try {
      s.setInt(10, 0);
      fail("set_int on an empty stack");
    } catch (Exception e) {
    }

    s.push(BLOCK_SIZE);
    s.setInt(10, 0);
    s.setInt(11, 1);

    assertEquals(10, s.getInt(0));
    assertEquals(11, s.getInt(1));

    s.push(BLOCK_SIZE);
    s.setInt(12, 0);
    s.setInt(13, 1);
    assertEquals(12, s.getInt(0));
    assertEquals(13, s.getInt(1));

    s.pop();
    assertEquals(10, s.getInt(0));
    assertEquals(11, s.getInt(1));
  }

  @Test
  public void testSet_long() {
    Stack s = new Stack(2, BLOCK_SIZE);
    try {
      s.setLong(Integer.MAX_VALUE + 1L, 0);
      fail("set_long on an empty stack");
    } catch (Exception e) {
    }

    s.push(BLOCK_SIZE);
    s.setLong(Integer.MAX_VALUE + 1L, 0);
    assertEquals(Integer.MAX_VALUE + 1L, s.getLong(0));

    s.push(BLOCK_SIZE);
    s.setLong(Integer.MAX_VALUE + 2L, 0);
    assertEquals(Integer.MAX_VALUE + 2L, s.getLong(0));

    s.pop();
    assertEquals(Integer.MAX_VALUE + 1L, s.getLong(0));
  }

  @Test
  public void testSet_ints() {

    Stack s = new Stack(2, BLOCK_SIZE);
    try {
      s.setLong(Integer.MAX_VALUE + 1L, 0);
      fail("set_ints on an empty stack");
    } catch (Exception e) {
    }

    int[] _123 = new int[] { 1, 2, 3 };
    int[] _456 = new int[] { 4, 5, 6 };
    int buffer[] = new int[3];

    s.push(BLOCK_SIZE);
    s.set_ints(_123, 0, 3, 0);
    s.getInts(0, buffer, 0, 3);
    assertArrayEquals(_123, buffer);

    s.push(BLOCK_SIZE);
    s.set_ints(_456, 0, 3, 0);
    s.getInts(0, buffer, 0, 3);
    assertArrayEquals(_456, buffer);

    s.pop();
    s.getInts(0, buffer, 0, 3);
    assertArrayEquals(_123, buffer);
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
