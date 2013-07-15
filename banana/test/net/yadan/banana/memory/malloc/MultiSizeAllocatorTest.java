package net.yadan.banana.memory.malloc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import net.yadan.banana.memory.initializers.MemSetInitializer;

import org.junit.Test;

public class MultiSizeAllocatorTest {

  @Test
  public void testGetReservedBits() {
    assertEquals(1, new MultiSizeAllocator(10, new int[]{10, 20}, 2.0).getReservedBits());
    assertEquals(2, new MultiSizeAllocator(10, new int[]{1, 2, 3}, 2.0).getReservedBits());
    assertEquals(2, new MultiSizeAllocator(10, new int[]{1, 2, 3, 4}, 2.0).getReservedBits());
    assertEquals(3, new MultiSizeAllocator(10, new int[]{1, 2, 3, 4, 5}, 2.0).getReservedBits());
  }

  @Test
  public void testFindAllocator() {
    MultiSizeAllocator m = new MultiSizeAllocator(10, new int[]{10, 20}, 2.0);

    assertEquals(0, m.findAllocatorFor(1));
    assertEquals(0, m.findAllocatorFor(10));

    assertEquals(1, m.findAllocatorFor(11));
    assertEquals(1, m.findAllocatorFor(20));

    try {
      m.findAllocatorFor(21); // should throw
      fail("Found an allocator fot size 21, should have failed");
    } catch (Exception e) {
    }
  }

  @Test
  public void testPointer() {

    int reserved = 3;
    int allocation = 32 - reserved;
    int[] sizes = new int[(int) Math.pow(2, reserved)];
    for (int i = 0; i < sizes.length; i++) {
      sizes[i] = i + 10;
    }

    MultiSizeAllocator m = new MultiSizeAllocator(10, sizes, 2.0);
    for (int idx = 0; idx < Math.pow(2, reserved); idx++) {
      for (int ptx = 0; ptx < allocation; ptx++) {
        int pointer = 1 << ptx;
        int ppp = m.encodePointer(idx, pointer);
        assertEquals(idx, m.getSizeIndex(ppp));
        assertEquals(pointer, m.extractPointer(ppp));
      }
    }
  }

  @Test
  public void testAllocations() {
    MultiSizeAllocator a = new MultiSizeAllocator(1, new int[]{32, 64, 128}, 2.0);
    int p1 = a.malloc(10);
    int p2 = a.malloc(40);
    a.setInt(p1, 0, 1);
    a.setInt(p2, 0, 2);
    assertEquals(1, a.getInt(p1, 0));
    assertEquals(2, a.getInt(p2, 0));

    int p3 = a.malloc(11); // growth!
    int p4 = a.malloc(41); // growth!
    a.setInt(p3, 0, 3);
    a.setInt(p4, 0, 4);
    assertEquals(1, a.getInt(p1, 0));
    assertEquals(2, a.getInt(p2, 0));
    assertEquals(3, a.getInt(p3, 0));
    assertEquals(4, a.getInt(p4, 0));
  }

  @Test
  public void testUpperShort() {
    MultiSizeAllocator a = new MultiSizeAllocator(1, new int[] { 32, 64, 128 }, 2.0);
    int p = a.malloc(10);
    assertEquals(0, a.getLowerShort(p, 0));
    assertEquals(0, a.getUpperShort(p, 0));
    a.setUpperShort(p, 0, 99);
    assertEquals(0, a.getLowerShort(p, 0));
    assertEquals(99, a.getUpperShort(p, 0));
    a.free(p);
  }

  @Test
  public void testLowerShort() {
    MultiSizeAllocator a = new MultiSizeAllocator(1, new int[] { 32, 64, 128 }, 2.0);
    int p = a.malloc(10);
    a.setInt(p, 0, 0);
    assertEquals(0, a.getLowerShort(p, 0));
    assertEquals(0, a.getUpperShort(p, 0));
    a.setLowerShort(p, 0, 99);
    assertEquals(99, a.getLowerShort(p, 0));
    assertEquals(0, a.getUpperShort(p, 0));
    a.free(p);
  }

  @Test
  public void testInitialize() {
    MultiSizeAllocator a = new MultiSizeAllocator(1, new int[]{32, 64, 128}, 2.0);
    a.setInitializer(new MemSetInitializer(-1));
    int size = 50;
    int p = a.malloc(size);
    try {
      int expected[] = new int[size]; // initialized to zeros
      a.memSet(p, 0, size, 0); // all zeros
      int out[] = new int[size];
      a.getInts(p, 0, out, 0, size);
      assertArrayEquals(expected, out);
      a.initialize(p);
      a.getInts(p, 0, out, 0, size);
      for (int i = 0; i < size; i++) {
        assertEquals(-1, out[i]);
      }
    } finally {
      a.free(p);
    }
  }
}
