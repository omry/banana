package net.yadan.banana.memory.block;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import junit.framework.TestSuite;
import net.yadan.banana.memory.IBlockAllocator;
import net.yadan.banana.memory.MemInitializer;
import net.yadan.banana.memory.OutOfMemoryException;
import net.yadan.banana.memory.initializers.MemSetInitializer;
import net.yadan.banana.memory.initializers.PrototypeInitializer;

import org.junit.After;
import org.junit.Test;

public abstract class AbstractBlockAllocatorTest extends TestSuite {

  IBlockAllocator a;

  private IBlockAllocator create(int numBlocks, int blockSize) {
    return create(numBlocks, blockSize, 0, new MemSetInitializer(-1));
  }

  public abstract IBlockAllocator create(int numBlocks, int blockSize, double growthFactor, MemInitializer initializer);

  @After
  public void postTest() {
    if (a != null) {
      assertEquals("Test leaks memory", 0, a.usedBlocks());
    }
    a = null;
  }

  @Test
  public void testConstructor() {
    a = create(20, 10);
    assertEquals(20, a.maxBlocks());
    assertEquals(10, a.blockSize());
  }

  @Test
  public void testAllocationsAndFreeBlocks_1() {
    testAllocationsAndFreeBlocks(1);
  }

  @Test
  public void testAllocationsAndFreeBlocks_2() {
    testAllocationsAndFreeBlocks(2);
  }

  @Test
  public void testAllocationsAndFreeBlocks_10() {
    testAllocationsAndFreeBlocks(10);
  }

  public void testAllocationsAndFreeBlocks(int size) {

    IBlockAllocator a = create(3, size);
    assertEquals(3, a.freeBlocks());
    a.malloc();
    assertEquals(2, a.freeBlocks());
    a.malloc();
    assertEquals(1, a.freeBlocks());
    a.malloc();
    assertEquals(0, a.freeBlocks());
    try {
      a.malloc();
      fail("Did not throw an OutOfMemoryException");
    } catch (OutOfMemoryException e) {
    }
  }

  @Test
  public void testFree() {
    a = create(2, 1);
    int p = a.malloc();
    assertEquals(1, a.freeBlocks());
    a.free(p);
    assertEquals(2, a.freeBlocks());
  }

  @Test
  public void testSetGetInt() {
    a = create(3, 1);
    int p1 = a.malloc();
    int p2 = a.malloc();
    int p3 = a.malloc();
    assertEquals(0, a.freeBlocks());
    a.setInt(p1, 0, 1);
    a.setInt(p2, 0, 2);
    a.setInt(p3, 0, 3);
    assertEquals(1, a.getInt(p1, 0));
    assertEquals(2, a.getInt(p2, 0));
    assertEquals(3, a.getInt(p3, 0));
    a.free(p1);
    a.free(p2);
    a.free(p3);
  }

  @Test
  public void testSetGetFloat() {
    a = create(3, 2);
    int p1 = a.malloc();
    int p2 = a.malloc();
    int p3 = a.malloc();
    assertEquals(0, a.freeBlocks());

    a.setFloat(p1, 0, 1);
    a.setFloat(p1, 1, 2);

    a.setFloat(p2, 0, Float.MIN_NORMAL);
    a.setFloat(p2, 1, Float.MAX_VALUE);

    a.setFloat(p3, 0, Float.NEGATIVE_INFINITY);
    a.setFloat(p3, 1, Float.POSITIVE_INFINITY);

    assertEquals(1, a.getFloat(p1, 0), Float.MIN_VALUE);
    assertEquals(2, a.getFloat(p1, 1), Float.MIN_VALUE);

    assertEquals(Float.MIN_NORMAL, a.getFloat(p2, 0), Float.MIN_VALUE);
    assertEquals(Float.MAX_VALUE, a.getFloat(p2, 1), Float.MIN_VALUE);

    assertEquals(Float.NEGATIVE_INFINITY, a.getFloat(p3, 0), Float.MIN_VALUE);
    assertEquals(Float.POSITIVE_INFINITY, a.getFloat(p3, 1), Float.MIN_VALUE);
    a.free(p1);
    a.free(p2);
    a.free(p3);
  }

  @Test
  public void testSetGetDouble() {
    a = create(3, 4);
    int p1 = a.malloc();
    int p2 = a.malloc();
    int p3 = a.malloc();
    assertEquals(0, a.freeBlocks());

    a.setDouble(p1, 0, 1);
    a.setDouble(p1, 2, 2);

    a.setDouble(p2, 0, Double.MIN_NORMAL);
    a.setDouble(p2, 2, Double.MAX_VALUE);

    a.setDouble(p3, 0, Double.NEGATIVE_INFINITY);
    a.setDouble(p3, 2, Double.POSITIVE_INFINITY);

    assertEquals(1, a.getDouble(p1, 0), Double.MIN_VALUE);
    assertEquals(2, a.getDouble(p1, 2), Double.MIN_VALUE);

    assertEquals(Double.MIN_NORMAL, a.getDouble(p2, 0), Double.MIN_VALUE);
    assertEquals(Double.MAX_VALUE, a.getDouble(p2, 2), Double.MIN_VALUE);

    assertEquals(Double.NEGATIVE_INFINITY, a.getDouble(p3, 0), Double.MIN_VALUE);
    assertEquals(Double.POSITIVE_INFINITY, a.getDouble(p3, 2), Double.MIN_VALUE);
    a.free(p1);
    a.free(p2);
    a.free(p3);
  }

  @Test
  public void testUpperShort() {
    a = create(3, 1);
    int p = a.malloc();
    a.setInt(p, 0, 0);
    assertEquals(0, a.getLowerShort(p, 0));
    assertEquals(0, a.getUpperShort(p, 0));
    a.setUpperShort(p, 0, 99);
    assertEquals(0, a.getLowerShort(p, 0));
    assertEquals(99, a.getUpperShort(p, 0));
    a.free(p);
  }

  @Test
  public void testLowerShort() {
    a = create(3, 1);
    int p = a.malloc();
    a.setInt(p, 0, 0);
    assertEquals(0, a.getLowerShort(p, 0));
    assertEquals(0, a.getUpperShort(p, 0));
    a.setLowerShort(p, 0, 99);
    assertEquals(99, a.getLowerShort(p, 0));
    assertEquals(0, a.getUpperShort(p, 0));
    a.free(p);
  }

  @Test
  public void testUpperShortNeg() {
    a = create(3, 1);
    int p = a.malloc();
    a.setInt(p, 0, 0);
    assertEquals(0, a.getLowerShort(p, 0));
    assertEquals(0, a.getUpperShort(p, 0));
    a.setUpperShort(p, 0, -1);
    assertEquals(0, a.getLowerShort(p, 0));
    assertEquals(-1, a.getUpperShort(p, 0));
    a.free(p);
  }

  @Test
  public void testLowerShortNeg() {
    a = create(3, 1);
    int p = a.malloc();
    a.setInt(p, 0, 0);
    assertEquals(0, a.getLowerShort(p, 0));
    assertEquals(0, a.getUpperShort(p, 0));
    a.setLowerShort(p, 0, -1);
    assertEquals(-1, a.getLowerShort(p, 0));
    assertEquals(0, a.getUpperShort(p, 0));
    a.free(p);
  }

  @Test
  public void testUpperShort_initialNeg() {
    a = create(3, 1);
    int p = a.malloc();
    a.setInt(p, 0, -1);
    assertEquals(-1, a.getLowerShort(p, 0));
    assertEquals(-1, a.getUpperShort(p, 0));
    a.setUpperShort(p, 0, 0);
    assertEquals(-1, a.getLowerShort(p, 0));
    assertEquals(0, a.getUpperShort(p, 0));
    a.free(p);
  }

  @Test
  public void testLowerShort_initialNeg() {
    a = create(3, 1);
    int p = a.malloc();
    a.setInt(p, 0, -1);
    assertEquals(-1, a.getLowerShort(p, 0));
    assertEquals(-1, a.getUpperShort(p, 0));
    a.setLowerShort(p, 0, 0);
    assertEquals(0, a.getLowerShort(p, 0));
    assertEquals(-1, a.getUpperShort(p, 0));
    a.free(p);
  }

  @Test
  public void testCharsAccessFullBlock() {
    a = create(3, 2);
    int p = a.malloc();
    char chars[] = new char[4];
    char out[] = new char[4];

    for (char a = 'a'; a - 'a' < 4; a++) {
      chars[a - 'a'] = a;
    }
    a.setChars(p, 0, chars, 0, 4);

    a.getChars(p, 0, out, 0, 4);

    assertArrayEquals(chars, out);

    a.free(p);
  }

  @Test
  public void testCharsSetPartialBlock() {
    a = create(3, 20);
    // new blocks will be initialized with -1
    a.setInitializer(new MemSetInitializer(-1));

    int p = a.malloc(); // 20 ints
    char chars[] = new char[20];
    char out[] = new char[20];

    for (char a = 'a'; a - 'a' < 20; a++) {
      chars[a - 'a'] = a;
    }
    a.setChars(p, 5, chars, 0, 20);

    a.getChars(p, 5, out, 0, 20);

    assertArrayEquals(chars, out);
    for (int i = 0; i < 5; i++) {
      assertEquals(-1, a.getInt(p, i));
    }

    for (int i = 15; i < 20; i++) {
      assertEquals(-1, a.getInt(p, i));
    }

    a.free(p);
  }

  @Test
  public void testCharsAccessFullBlock_even() {
    a = create(1, 2);
    // new blocks will be initialized with -1
    a.setInitializer(new MemSetInitializer(-1));

    // set 19 chars, last int should be partial
    int p = a.malloc();
    int n = 3;
    char chars[] = new char[n];
    char out[] = new char[n];

    for (char a = 'a'; a - 'a' < n; a++) {
      chars[a - 'a'] = a;
    }
    a.setChars(p, 0, chars, 0, n);

    a.getChars(p, 0, out, 0, n);

    assertArrayEquals(chars, out);

    assertEquals(-1, a.getLowerShort(p, 1));

    a.free(p);
  }

  @Test
  public void testLong() {
    a = create(10, 5);
    int p = a.malloc();
    a.setLong(p, 0, Long.MAX_VALUE);
    assertEquals(Long.MAX_VALUE, a.getLong(p, 0));
    a.free(p);
  }

  @Test
  public void testInitializer() {
    int[] prototype = new int[] { 1, 2, 3 };
    a = create(5, 3, 0, new PrototypeInitializer(prototype));

    for (int i = 0; i < 5; i++) {
      int n = a.malloc();
      int[] data = new int[3];
      a.getInts(n, 0, data, 0, 3);
      assertArrayEquals(prototype, data);
      a.free(n);
    }
  }

  @Test
  public void testGrowth() {
    a = create(2, 1, 2.0, null);
    int p1 = a.malloc();
    int p2 = a.malloc();
    a.setInt(p1, 0, 1);
    a.setInt(p2, 0, 2);
    assertEquals(2, a.maxBlocks());
    assertEquals(2, a.usedBlocks());
    assertEquals(0, a.freeBlocks());

    int p3 = a.malloc(); // growth!
    a.setInt(p3, 0, 3);

    assertEquals(4, a.maxBlocks());
    assertEquals(3, a.usedBlocks());
    assertEquals(1, a.freeBlocks());

    int p4 = a.malloc();
    a.setInt(p4, 0, 4);

    assertEquals(4, a.maxBlocks());
    assertEquals(4, a.usedBlocks());
    assertEquals(0, a.freeBlocks());

    a.free(p1);
    a.free(p2);
    a.free(p3);
    a.free(p4);
  }

  @Test
  public void testMemCopy_fullblock() {
    a = create(2, 5, 0, null);

    int[] data = new int[] { 1, 2, 3, 4, 5 };
    int[] out = new int[5];

    int p1 = a.malloc();
    int p2 = a.malloc();

    a.setInts(p1, 0, data, 0, a.blockSize());
    a.memCopy(p1, 0, p2, 0, a.blockSize());

    a.getInts(p2, 0, out, 0, a.blockSize());

    assertArrayEquals(data, out);

    a.free(p1);
    a.free(p2);
  }

  @Test
  public void testMemCopy_same_offset() {
    a = create(2, 5, 0, null);

    int[] data = new int[] { 1, 2, 3, 4, 5 };
    int[] expected = new int[] { 0, 2, 3, 4, 5 };
    int[] out = new int[5];

    int p1 = a.malloc();
    int p2 = a.malloc();

    a.setInts(p1, 0, data, 0, a.blockSize());
    a.memCopy(p1, 1, p2, 1, a.blockSize() - 1);

    a.getInts(p2, 0, out, 0, a.blockSize());

    assertArrayEquals(expected, out);

    a.free(p1);
    a.free(p2);
  }

  @Test
  public void testMemCopy_diff_offset() {
    a = create(2, 5, 0, null);

    int[] data = new int[] { 1, 2, 3, 4, 5 };
    int[] expected = new int[] { 0, 1, 2, 3, 4 };
    int[] out = new int[5];

    int p1 = a.malloc();
    int p2 = a.malloc();

    a.setInts(p1, 0, data, 0, a.blockSize());
    a.memCopy(p1, 0, p2, 1, a.blockSize() - 1);

    a.getInts(p2, 0, out, 0, a.blockSize());

    assertArrayEquals(expected, out);

    a.free(p1);
    a.free(p2);
  }

  @Test
  public void testMemSet_fullblock() {
    a = create(2, 5, 0, null);

    int[] data = new int[] { 1, 2, 3, 4, 5 };
    int[] expected = new int[] { 9, 9, 9, 9, 9 };
    int[] out = new int[5];
    int p1 = a.malloc();
    a.setInts(p1, 0, data, 0, a.blockSize());
    a.memSet(p1, 0, 5, 9);

    a.getInts(p1, 0, out, 0, a.blockSize());

    assertArrayEquals(expected, out);

    a.free(p1);
  }

  @Test
  public void testMemSet_partialBlock() {
    a = create(2, 5, 0, null);

    int[] data = new int[] { 1, 2, 3, 4, 5 };
    int[] expected = new int[] { 1, 9, 9, 9, 5 };
    int[] out = new int[5];
    int p1 = a.malloc();
    a.setInts(p1, 0, data, 0, a.blockSize());
    a.memSet(p1, 1, 3, 9);

    a.getInts(p1, 0, out, 0, a.blockSize());

    assertArrayEquals(expected, out);

    a.free(p1);
  }

  @Test
  public void testInitialize() {
    int[] prototype = new int[] { 1, 2, 3 };
    a = create(5, 3, 0, new PrototypeInitializer(prototype));
    int p = a.malloc();
    try {
      a.memSet(p, 0, 3, 0);
      int out[] = new int[3];
      a.getInts(p, 0, out, 0, 3);
      assertArrayEquals(new int[] { 0, 0, 0 }, out);
      a.initialize(p);
      a.getInts(p, 0, out, 0, 3);
      assertArrayEquals(prototype, out);
    } finally {
      a.free(p);
    }
  }

  @Override
  public String toString() {
    return a.toString();
  }

  @Test
  public void testMemCopy_fullblock_diffArrays() {
    a = create(8, 5, 0, null);

    int[] data = new int[] { 1, 2, 3, 4, 5 };
    int[] expected = new int[] { 1, 2, 3, 4, 5 };
    int[] out = new int[5];

    int a1_1 = a.malloc();
    a.malloc();
    a.malloc();
    a.malloc();
    int a2_1 = a.malloc();

    a.setInts(a1_1, 0, data, 0, a.blockSize());
    a.memCopy(a1_1, 0, a2_1, 0, a.blockSize());

    a.getInts(a2_1, 0, out, 0, a.blockSize());

    assertArrayEquals(expected, out);

    a.clear();
  }

  @Test
  public void testMemCopy_same_offset_diffArrays() {
    a = create(8, 5, 0, null);

    int[] data = new int[] { 1, 2, 3, 4, 5 };
    int[] expected = new int[] { 0, 2, 3, 4, 5 };
    int[] out = new int[5];

    int a1_1 = a.malloc();
    a.malloc();
    a.malloc();
    a.malloc();
    int a2_1 = a.malloc();

    a.setInts(a1_1, 0, data, 0, a.blockSize());
    a.memCopy(a1_1, 1, a2_1, 1, a.blockSize() - 1);

    a.getInts(a2_1, 0, out, 0, a.blockSize());

    assertArrayEquals(expected, out);

    a.clear();
  }

  @Test
  public void testMemCopy_diff_offset_diffArrays() {
    a = create(8, 5, 0, null);

    int[] data = new int[] { 1, 2, 3, 4, 5 };
    int[] expected = new int[] { 0, 1, 2, 3, 4 };
    int[] out = new int[5];

    int a1_1 = a.malloc();
    a.malloc();
    a.malloc();
    a.malloc();
    int a2_1 = a.malloc();

    a.setInts(a1_1, 0, data, 0, a.blockSize());
    a.memCopy(a1_1, 0, a2_1, 1, a.blockSize() - 1);

    a.getInts(a2_1, 0, out, 0, a.blockSize());

    assertArrayEquals(expected, out);

    a.clear();
  }
}
