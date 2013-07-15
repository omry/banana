package net.yadan.banana.list;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import net.yadan.banana.memory.Buffer;
import net.yadan.banana.memory.IBuffer;

import org.junit.Test;


public class LinkedListTest {

  public ILinkedList createList(int maxBlocks, int blockSize, double growthFactor) {
    LinkedList list = new LinkedList(maxBlocks, blockSize, growthFactor);
    list.getAllocator();
    return list;
  }

  public ILinkedList createList(int maxBlocks, int sizes[], double growthFactor) {
    return new LinkedList(maxBlocks, sizes, growthFactor);
  }

  @Test
  public void testInsertHead() {
    ILinkedList list = createList(2, 1, 0);
    assertEquals(-1, list.getHead());
    int pointer = list.insertHead(1);
    list.setInt(pointer, 0, 10);
    assertEquals(pointer, list.getHead());
    assertEquals(10, list.getInt(pointer, 0));
  }

  @Test
  public void testRemoveHead() {
    ILinkedList list = createList(2, 1, 0);
    int link1 = list.insertHead(1);
    list.setInt(link1, 0, 10);

    int link2 = list.insertHead(1);
    list.setInt(link2, 0, 11);

    assertEquals(link2, list.getHead());
    assertEquals(11, list.getInt(list.getHead(), 0));

    list.removeHead();

    assertEquals(link1, list.getHead());
    assertEquals(10, list.getInt(list.getHead(), 0));

    list.removeHead();
    assertEquals(-1, list.getHead());
  }

  @Test
  public void testAppendTail() {
    ILinkedList list = createList(2, 1, 0);
    assertEquals(-1, list.getTail());
    int link1 = list.appendTail(1);
    assertEquals(link1, list.getHead());
    assertEquals(link1, list.getTail());
    assertEquals(-1, list.getNext(list.getTail()));

    int link2 = list.appendTail(1);
    assertEquals(link1, list.getHead());
    assertEquals(link2, list.getNext(link1));
    assertEquals(link2, list.getTail());
    assertEquals(-1, list.getNext(list.getTail()));
  }

  @Test
  public void testRemove() {
    ILinkedList list = createList(3, 1, 0);
    int link1 = list.insertHead(1);
    list.setInt(link1, 0, 10);

    int link2 = list.insertHead(1);
    list.setInt(link2, 0, 11);

    int link3 = list.insertHead(1);
    list.setInt(link3, 0, 12);

    assertEquals(link3, list.getHead());
    assertEquals(link2, list.getNext(link3));
    assertEquals(link1, list.getNext(link2));
    assertEquals(-1, list.getNext(link1));

    list.remove(link2);
    assertEquals(link3, list.getHead());
    assertEquals(link1, list.getNext(link3));
    assertEquals(-1, list.getNext(link1));

    list.remove(link1);
    assertEquals(link3, list.getHead());
    assertEquals(-1, list.getNext(link3));

    list.remove(link3);
    assertEquals(-1, list.getHead());
  }

  @Test
  public void testGetInt() {
    ILinkedList list = createList(3, 2, 0);
    int link = list.insertHead(2);
    list.setInt(link, 0, 10);
    list.setInt(link, 1, 11);

    assertEquals(10, list.getInt(link, 0));
    assertEquals(11, list.getInt(link, 1));
  }

  @Test
  public void testSetInts() {
    ILinkedList list = createList(3, 5, 0);
    int link1 = list.insertHead(5);
    int[] data = new int[] { 1, 2, 3, 4, 5 };
    list.setInts(link1, 0, data, 0, data.length);

    int res[] = new int[data.length];
    list.getInts(link1, 0, res, 0, data.length);
    assertArrayEquals(data, res);
  }

  @Test
  public void testGetLong() {
    ILinkedList list = createList(3, 4, 0);
    int link = list.insertHead(4);
    list.setLong(link, 0, 10L);
    list.setLong(link, 2, 11L);

    assertEquals(10, list.getLong(link, 0));
    assertEquals(11, list.getLong(link, 2));
  }

  @Test
  public void testVarBlockList() {
    ILinkedList list = createList(10, new int[] { 5, 10 }, 0);
    try {
      list.appendTail(200);
      fail();
    } catch (Exception e) {
    }

    int n1 = list.appendTail(1);
    list.setInt(n1, 0, 10);

    int n2 = list.appendTail(10);
    for (int i = 0; i < 10; i++) {
      list.setInt(n2, i, i);
    }

    for (int i = 0; i < 10; i++) {
      assertEquals(i, list.getInt(n2, i));
    }
  }

  @Test
  public void testInsert() {
    ILinkedList list = createList(5, 1, 0);
    int n1 = list.insert(1, list.getHead());
    assertEquals(n1, list.getHead());
    assertEquals(n1, list.getTail());
    assertEquals(-1, list.getNext(n1));

    int n2 = list.insert(1, n1);
    assertEquals(n1, list.getHead());
    assertEquals(n2, list.getTail());
    assertEquals(n2, list.getNext(n1));
    assertEquals(-1, list.getNext(n2));

    int n3 = list.insert(1, n1);
    assertEquals(n1, list.getHead());
    assertEquals(n2, list.getTail());
    assertEquals(n3, list.getNext(n1));
    assertEquals(n2, list.getNext(n3));
    assertEquals(-1, list.getNext(n2));
  }

  @Test
  public void testSize() {
    ILinkedList list = createList(5, 1, 0);
    assertEquals(0, list.size());
    int n1 = list.appendTail(1);
    assertEquals(1, list.size());

    int n2 = list.insert(1, n1);
    assertEquals(2, list.size());

    list.insertHead(1);
    assertEquals(3, list.size());

    list.removeHead();
    assertEquals(2, list.size());

    list.remove(n1);
    assertEquals(1, list.size());

    list.remove(n2);
    assertEquals(0, list.size());
  }

  @Test
  public void testInsert_buffer(){
    ILinkedList list = createList(5, 5, 0);
    char[] chars = "Hello world".toCharArray();
    IBuffer buffer = new Buffer(10);
    IBuffer outBuffer = new Buffer(10);
    buffer.appendChars(chars);
    int node = list.insertHead(buffer);
    list.getBuffer(node, 0, outBuffer, buffer.size());
    assertEquals(buffer.size(), outBuffer.size());
    assertArrayEquals(buffer.array(), outBuffer.array());
  }

  @Test
  public void testInsertHead_buffer() {
    ILinkedList list = createList(5, 5, 0);
    char[] chars = "Hello world".toCharArray();
    IBuffer buffer = new Buffer(10);
    IBuffer outBuffer = new Buffer(10);
    buffer.appendChars(chars);
    int node = list.insertHead(buffer);
    list.getBuffer(node, 0, outBuffer, buffer.size());
    assertEquals(buffer.size(), outBuffer.size());
    assertArrayEquals(buffer.array(), outBuffer.array());
  }

  @Test
  public void testappendTail_buffer() {
    ILinkedList list = createList(5, 5, 0);
    char[] chars = "Hello world".toCharArray();
    IBuffer buffer = new Buffer(10);
    IBuffer outBuffer = new Buffer(10);
    buffer.appendChars(chars);
    int node = list.appendTail(buffer);
    list.getBuffer(node, 0, outBuffer, buffer.size());
    assertEquals(buffer.size(), outBuffer.size());
    assertArrayEquals(buffer.array(), outBuffer.array());
  }

  @Test
  public void testClear() {
    ILinkedList list = createList(5, 1, 0);
    assertEquals(0, list.size());
    int n1 = list.appendTail(1);
    assertEquals(1, list.size());
    list.insert(1, n1);
    assertEquals(2, list.size());

    list.clear();
    assertEquals(0, list.size());
    assertEquals(0, list.getAllocator().usedBlocks());
  }
}
