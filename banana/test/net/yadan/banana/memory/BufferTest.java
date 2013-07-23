package net.yadan.banana.memory;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BufferTest {

  private IBuffer create(int size) {
    return new Buffer(size);
  }

  private IBuffer create(int size, double growthFactor) {
    return new Buffer(size, growthFactor);
  }

  @Test
  public void testBufferInt() {
    IBuffer b = create(10);
    assertEquals(10, b.capacity());
    assertEquals(0, b.size());
    assertEquals(2, b.getGrowthFactor(), Double.MIN_VALUE);
  }

  @Test
  public void testBufferIntDouble() {
    IBuffer b = create(10, 1.5);
    assertEquals(10, b.capacity());
    assertEquals(0, b.size());
    assertEquals(1.5, b.getGrowthFactor(), Double.MIN_VALUE);

  }

  @Test
  public void testSize() {
    IBuffer b = create(10);
    assertEquals(0, b.size());
    b.appendInt(1);
    assertEquals(1, b.size());
  }

  @Test
  public void testReset() {
    IBuffer b = create(10);
    b.appendInt(1);
    b.reset();
    assertEquals(0, b.size());
  }

  @Test
  public void testEnsureCapacity() {
    IBuffer b = create(10, 2.0);
    assertEquals(10, b.capacity());
    b.ensureCapacity(5);
    assertEquals(10, b.capacity());
    b.ensureCapacity(15);
    assertEquals(20, b.capacity());
  }

  @Test
  public void testWriteWithEnsureCapacity() {
    IBuffer b = create(1, 2);
    assertEquals(1, b.capacity());

    b.appendInt(10);
    assertEquals(1, b.capacity());
    assertEquals(1, b.size());

    b.ensureCapacity(2);
    assertEquals(2, b.capacity());
    assertEquals(1, b.size());

    b.appendInt(11);
    assertEquals(2, b.size());

    assertEquals(10, b.getInt(0));
    assertEquals(11, b.getInt(1));
  }

  @Test
  public void testCapacity() {
    IBuffer b = create(10, 2);
    assertEquals(10, b.capacity());
    b.ensureCapacity(20);
    assertEquals(20, b.capacity());
  }

  @Test
  public void testUpperShort() {
    IBuffer b = create(1);
    assertEquals(0, b.getUpperShort(0));
    b.setUpperShort(0, 99);
    assertEquals(99, b.getUpperShort(0));
    assertEquals(1, b.size());
  }

  @Test
  public void testUpperShortAfterReset() {
    IBuffer b = create(1);
    b.setLowerShort(0, 10);
    b.setUpperShort(0, 20);
    assertEquals(10, b.getLowerShort(0));
    assertEquals(20, b.getUpperShort(0));
    assertEquals(1, b.size());

    // test upper after reset
    b.reset();
    assertEquals(0, b.size());
    b.setUpperShort(0, 20);
    assertEquals(0, b.getLowerShort(0));
    assertEquals(20, b.getUpperShort(0));
  }

  @Test
  public void testLowerShortAfterReset() {
    IBuffer b = create(1);
    b.setLowerShort(0, 10);
    b.setUpperShort(0, 20);
    assertEquals(10, b.getLowerShort(0));
    assertEquals(20, b.getUpperShort(0));
    assertEquals(1, b.size());

    // test upper after reset
    b.reset();
    assertEquals(0, b.size());
    b.setLowerShort(0, 10);
    assertEquals(10, b.getLowerShort(0));
    assertEquals(0, b.getUpperShort(0));
  }

  @Test
  public void testLowerShort() {
    IBuffer b = create(1);
    assertEquals(0, b.getLowerShort(0));
    b.setLowerShort(0, 99);
    assertEquals(99, b.getLowerShort(0));
    assertEquals(1, b.size());
  }

  @Test
  public void testUpperShortNeg() {
    IBuffer b = create(1);
    b.setInt(0, 0);
    assertEquals(0, b.getLowerShort(0));
    assertEquals(0, b.getUpperShort(0));
    b.setUpperShort(0, -1);
    assertEquals(0, b.getLowerShort(0));
    assertEquals(-1, b.getUpperShort(0));
  }

  @Test
  public void testLowerShortNeg() {
    IBuffer b = create(1);
    b.setInt(0, 0);
    assertEquals(0, b.getLowerShort(0));
    assertEquals(0, b.getUpperShort(0));
    b.setLowerShort(0, -1);
    assertEquals(-1, b.getLowerShort(0));
    assertEquals(0, b.getUpperShort(0));
  }

  @Test
  public void testUpperShort_initialNeg() {
    IBuffer b = create(1);
    b.setInt(0, -1);
    assertEquals(-1, b.getLowerShort(0));
    assertEquals(-1, b.getUpperShort(0));
    b.setUpperShort(0, 0);
    assertEquals(-1, b.getLowerShort(0));
    assertEquals(0, b.getUpperShort(0));
  }

  @Test
  public void testLowerShort_initialNeg() {
    IBuffer b = create(1);
    b.setInt(0, -1);
    assertEquals(-1, b.getLowerShort(0));
    assertEquals(-1, b.getUpperShort(0));
    b.setLowerShort(0, 0);
    assertEquals(0, b.getLowerShort(0));
    assertEquals(-1, b.getUpperShort(0));
  }

  @Test
  public void testInt() {
    IBuffer b = create(1);
    assertEquals(0, b.getInt(0));
    b.setInt(0, 99);
    assertEquals(99, b.getInt(0));
  }

  @Test
  public void testLong() {
    IBuffer b = create(2);
    assertEquals(0, b.getLong(0));
    b.setLong(0, Long.MAX_VALUE);
    assertEquals(Long.MAX_VALUE, b.getLong(0));
  }

  @Test
  public void testInts() {
    IBuffer b = create(10);
    int data[] = { 1, 2, 3, 4, 5 };
    int out[] = new int[5];
    b.setInts(0, data, 0, data.length);
    b.getInts(0, out, 0, data.length);
    assertArrayEquals(data, out);
  }

  @Test
  public void testAppendInt() {
    IBuffer b = create(10);
    assertEquals(0, b.size());
    b.appendInt(10);
    assertEquals(10, b.getInt(0));
    assertEquals(1, b.size());
  }

  @Test
  public void testAppendLong() {
    IBuffer b = create(10);
    assertEquals(0, b.size());
    b.appendLong(Long.MAX_VALUE);
    assertEquals(Long.MAX_VALUE, b.getLong(0));
    assertEquals(2, b.size());
  }

  @Test
  public void testAppendInts() {
    IBuffer b = create(10);
    assertEquals(0, b.size());
    int data[] = { 1, 2, 3, 4, 5 };
    int out[] = new int[5];
    b.appendInts(data, 0, data.length);
    assertEquals(data.length, b.size());
    b.getInts(0, out, 0, out.length);
    assertArrayEquals(data, out);
  }

  @Test
  public void testChars() {
    IBuffer b = create(10);
    char c[] = "hello there".toCharArray();
    char out[] = new char[c.length];
    b.setChars(0, c, 0, c.length);
    b.getChars(0, out, 0, out.length);

    assertArrayEquals(c, out);
  }

  @Test
  public void testCharsSrcOffset() {
    IBuffer b = create(10);
    char c[] = "hello there".toCharArray();
    char out[] = "hello -----".toCharArray();
    b.setChars(0, c, 6, c.length - 6);
    b.getChars(0, out, 6, out.length - 6);

    assertEquals(3, b.size());
    assertArrayEquals(c, out);
  }

  @Test
  public void testCharsDstOffset() {
    IBuffer b = create(10);
    char c[] = "hello there".toCharArray();
    char out[] = "hello -----".toCharArray();
    b.setInt(0, 999);
    b.setChars(1, c, 6, c.length - 6);
    b.getChars(1, out, 6, out.length - 6);
    assertEquals(4, b.size());

    assertArrayEquals(c, out);
    assertEquals(999, b.getInt(0));
  }

}
