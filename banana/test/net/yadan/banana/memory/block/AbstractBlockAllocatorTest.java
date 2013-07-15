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
}
