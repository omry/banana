package net.yadan.banana.memory.malloc.treeallocator;

import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.initializers.MemSetInitializer;
import net.yadan.banana.memory.malloc.TreeAllocator;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class VarTests {

  IMemAllocator m;

  @After
  public void postTest() {
    if (m != null) {
      assertEquals("Test leaks memory", 0, m.usedBlocks());
    }
    m = null;
  }

  @Test
  public void testAlloc21_3() {
    init(100, 3);
    int size = 21;
    int data[] = new int[size];
    int out[] = new int[size];
    for (int j = 0; j < data.length; j++) {
      data[j] = j;
    }

    int p = m.malloc(size);

    m.setInts(p, 0, data, 0, data.length);

    m.getInts(p, 0, out, 0, out.length);
    assertArrayEquals("Size : " + size, data, out);
    assertEquals(21, m.maximumCapacityFor(p));


    m.free(p);
  }

  protected void init(int maxBlocks, int blockSize) {
    m = new TreeAllocator(maxBlocks, blockSize);
    m.setInitializer(new MemSetInitializer(-1));
    m.setDebug(true);
  }
}
