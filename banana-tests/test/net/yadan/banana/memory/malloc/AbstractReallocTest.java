package net.yadan.banana.memory.malloc;

import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.OutOfMemoryException;
import net.yadan.banana.memory.initializers.MemSetInitializer;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;


@RunWith(value = Parameterized.class)
public abstract class AbstractReallocTest {

  public final boolean DEBUG = false;
  private int m_numBlocks;
  private int m_blockSize;
  private List<IMemAllocator> allocators;

  public AbstractReallocTest(int numBlocks, int blockSize) {
    init(numBlocks, blockSize);
  }

  public void init(int numBlocks, int blockSize) {
    m_numBlocks = numBlocks;
    m_blockSize = blockSize;
    allocators = new ArrayList<IMemAllocator>();
  }

  private IMemAllocator create() {
    IMemAllocator mm = createImpl(m_numBlocks, m_blockSize);
    allocators.add(mm);
    return mm;
  }

  public abstract IMemAllocator createImpl(int numBlocks, int blockSize);

  @Parameters
  public static Collection<Object[]> data() {
    //@formatter:off
    Object[][] data = new Object[][] {
        { 10  , 3},
        { 10  , 10},
        { 100 , 3},
        { 100 , 4},
        { 100 , 5},
//        { 1000 , 4},
    };
    return Arrays.asList(data);
  }
  //@formatter:on

  @After
  public void postTest() {
    for (IMemAllocator mm : allocators) {
      assertEquals("Test leaks memory", 0, mm.usedBlocks());
    }
    allocators.clear();
  }

  @Test
  public void testReallocOfSingleBlock_expand() {

    /*
     * Tests the case where expanding a single block
     */

    IMemAllocator m = create();
    int blockSize = m.blockSize();
    int data[] = new int[blockSize];
    int out[] = new int[blockSize];
    for (int i = 0; i < data.length; i++) {
      data[i] = i;
    }

    int p1 = m.malloc(blockSize);
    m.setInts(p1, 0, data, 0, data.length);
    int p2 = m.realloc(p1, blockSize * 2);
    m.getInts(p2, 0, out, 0, out.length);

    assertTrue(p1 > 0); // single block
    assertTrue(p2 < 0); // multi block
    assertTrue(blockSize * 2 <= m.maximumCapacityFor(p2));
    assertArrayEquals(data, out);

    m.free(p2); // don't free p1!
  }

  @Test
  public void testReallocSingleBlock_sameSize() {
    /*
     * Tests the case where requesting realloc for the same size for a single
     * block pointer
     */
    IMemAllocator m = create();
    int blockSize = m.blockSize();
    int data[] = new int[blockSize];
    int out[] = new int[blockSize];
    for (int i = 0; i < data.length; i++) {
      data[i] = i;
    }

    int p1 = m.malloc(blockSize);
    m.setInts(p1, 0, data, 0, data.length);
    int p2 = m.realloc(p1, blockSize);
    m.getInts(p2, 0, out, 0, out.length);

    assertEquals(p1, p2);// same pointer
    assertArrayEquals(data, out);

    m.free(p2); // don't free p1!
  }

  @Test
  public void testReallocMultiBlock_sameSize() {
    /*
     * Tests the case where requesting realloc for the same size for a multi
     * block
     */
    IMemAllocator m = create();
    int blockSize = m.blockSize();
    int data[] = new int[blockSize * 2];
    int out[] = new int[blockSize * 2];
    for (int i = 0; i < data.length; i++) {
      data[i] = i;
    }

    int p1 = m.malloc(blockSize * 2);
    m.setInts(p1, 0, data, 0, data.length);
    int p2 = m.realloc(p1, blockSize * 2);
    m.getInts(p2, 0, out, 0, out.length);

    assertEquals(p1, p2);// same pointer
    assertArrayEquals(data, out);

    m.free(p2); // don't free p1!
  }

  @Test
  public void testReallocMultiBlock_expand() {

    /*
     * Tests the case where expanding a multi block
     */
    IMemAllocator m = create();
    int blockSize = m.blockSize();
    int data[] = new int[blockSize * 2];
    int out[] = new int[blockSize];
    for (int i = 0; i < data.length; i++) {
      data[i] = i;
    }

    int p1 = m.malloc(blockSize * 2);
    m.setInts(p1, 0, data, 0, data.length);
    int p2 = m.realloc(p1, blockSize * 3);
    m.getInts(p2, 0, out, 0, out.length);

    assertTrue(blockSize * 3 <= m.maximumCapacityFor(p2));

    m.free(p2); // don't free p1!
  }

  @Test
  public void testReallocMultiBlock_shrink() {

    /*
     * Tests the case where shrinking a multi block to a muilti block
     */
    IMemAllocator m = create();
    int blockSize = m.blockSize();
    int data[] = new int[blockSize * 3];
    int out[] = new int[blockSize * 2];
    for (int i = 0; i < data.length; i++) {
      data[i] = i;
    }

    int p1 = m.malloc(blockSize * 3);
    m.setInts(p1, 0, data, 0, data.length);
    int p2 = m.realloc(p1, blockSize * 2);
    m.getInts(p2, 0, out, 0, out.length);

    assertTrue(p1 < 0); // multi block
    assertTrue(p2 < 0); // multi block
    int max = m.maximumCapacityFor(p2);
    assertTrue(blockSize * 3 > max);
    assertTrue(blockSize * 2 <= max);
    for (int i = 0; i < out.length; i++) {
      assertEquals(data[i], out[i]);
    }

    m.free(p2); // don't free p1!
  }

  @Test
  public void testRealloc_expand_multiblock_OOM() {

    /*
     * Tests the case where expanding a multi block and getting an OOM original
     * block should be unaffected and memory should not leak
     */
    IMemAllocator m = create();
    int alloc = m.blockSize() * m.maxBlocks() / 2;
    int data[] = new int[alloc];
    int out[] = new int[alloc];
    for (int i = 0; i < data.length; i++) {
      data[i] = i;
    }

    int p1 = m.malloc(alloc);
    int free = m.freeBlocks();
    m.setInts(p1, 0, data, 0, data.length);
    try {
      m.realloc(p1, alloc * 3);
      fail(); // should throw
    } catch (OutOfMemoryException e) {
    }

    assertEquals(free, m.freeBlocks());
    m.getInts(p1, 0, out, 0, out.length);
    assertArrayEquals(data, out);

    assertTrue(alloc <= m.maximumCapacityFor(p1));
    m.free(p1);
  }

  @Test
  public void testRealloc_expand_singleblock_OOM() {

    /*
     * Tests the case where expanding a single block and getting an OOM original
     * block should be unaffected and memory should not leak
     */
    IMemAllocator m = create();
    int alloc = m.blockSize();
    int data[] = new int[alloc];
    int out[] = new int[alloc];
    for (int i = 0; i < data.length; i++) {
      data[i] = i;
    }

    int p1 = m.malloc(alloc);
    int free = m.freeBlocks();
    m.setInts(p1, 0, data, 0, data.length);
    try {
      p1 = m.realloc(p1, m.blockSize() * (m.maxBlocks() + 5));
      fail(); // should throw
    } catch (OutOfMemoryException e) {
    }

    assertEquals(free, m.freeBlocks());
    m.getInts(p1, 0, out, 0, out.length);
    assertArrayEquals(data, out);

    assertTrue(alloc <= m.maximumCapacityFor(p1));
    m.free(p1);
  }

  @Test
  public void reallocTestLoop() {
    IMemAllocator m = create();
    int blockSize = m.blockSize();
    int size = blockSize;
    int pointer = m.malloc(size);
    int max = m.maxBlocks();
    try {
      for (int i = 0; i < max; i++) {
        int data[] = new int[size];
        int out[] = new int[size];
        for (int j = 0; j < data.length; j++) {
          data[j] = j;
        }

        m.setInts(pointer, 0, data, 0, data.length);
        size += blockSize;
        pointer = m.realloc(pointer, size);
        m.getInts(pointer, 0, out, 0, out.length);
        assertArrayEquals(data, out);

        assertTrue(m.maximumCapacityFor(pointer) >= size);
      }
    } catch (OutOfMemoryException e) {
      // expected due to allocators overhead, just run till we find max
      // allocation size
    }

    m.free(pointer);
  }

  /**
   * Full spectrum realloc OOM test that ensures memory structure and content
   * are unmodified if an OOM is thrown during realloc
   */
  @Test
  public void testRealloc_singleBlockIncrement_OOM_loop() {
    IMemAllocator m = create();
    int blockSize = m.blockSize();
    int max = m.maxBlocks();
    int data[] = new int[max * blockSize];
    int out[] = new int[max * blockSize];
    for (int j = 0; j < data.length; j++) {
      data[j] = j;
    }

    int initialBlockCount = 0;
    int realllocFirstValue = 0;
    int reallocBlockCount = -1;
    boolean exit = false;
    try {
      for (; initialBlockCount < max && !exit; initialBlockCount++) {
        if (reallocBlockCount != -1) {
          reallocBlockCount = 0;
        } else {
          reallocBlockCount = realllocFirstValue;
        }
        for (; reallocBlockCount < max && !exit; reallocBlockCount++) {
          int initialSize = initialBlockCount * blockSize;
          int p1;
          try {
            p1 = m.malloc(initialSize);
          } catch (OutOfMemoryException e) {
            // can't allocate initial buffer, we are done
            exit = true;
            break;
          }
          int free = m.freeBlocks();
          m.setInts(p1, 0, data, 0, initialSize);

          String beforeRealloc = m.pointerDebugString(p1);
          if (DEBUG) {
            System.out.println("Before realloc:\n" + beforeRealloc);
          }

          try {
            p1 = m.realloc(p1, reallocBlockCount * blockSize);
            m.free(p1); // if didn't throw, move to next loop
            continue;
          } catch (OutOfMemoryException e) {
          }

          String afterRealloc = m.pointerDebugString(p1);
          if (DEBUG) {
            System.out.println("After realloc:\n" + afterRealloc);
          }

          assertEquals(free, m.freeBlocks());

          assertEquals(beforeRealloc, afterRealloc);

          assertEquals(free, m.freeBlocks());
          m.getInts(p1, 0, out, 0, initialSize);
          for (int k = 0; k < initialSize; k++) {
            assertEquals(data[k], out[k]);
          }

          assertTrue(initialSize <= m.maximumCapacityFor(p1));
          m.free(p1);
        }
      }
    } catch (AssertionError e) {
      throw new AssertionError(String.format("Error in %d -> %d", initialBlockCount,
          reallocBlockCount), e);
    }

    assertEquals("Test leaks memory", 0, m.usedBlocks());
  }

  /**
   * Full spectrum realloc test for increasing memory size
   */
  @Test
  public void reallocAddSingleBlockTest() {
    int maxBlocks = m_numBlocks;
    int blockSize = m_blockSize;
    IMemAllocator m1 = create();
    IMemAllocator m2 = create();
    for (int n = 0; n < maxBlocks; n++) {
      int p1 = -1;
      int p2 = -1;
      try {
        int size = n * blockSize;
        int newSize = (n + 1) * blockSize;
        m1.setInitializer(new MemSetInitializer(-1));
        m1.setDebug(true);

        m2.setInitializer(new MemSetInitializer(-1));
        m2.setDebug(true);

        int data[] = new int[size];
        int out[] = new int[size];
        for (int j = 0; j < data.length; j++) {
          data[j] = j;
        }

        p2 = m2.malloc(newSize);
        m2.setInts(p2, 0, data, 0, data.length);

        p1 = m1.malloc(size);
        m1.setInts(p1, 0, data, 0, data.length);

        if (DEBUG) {
          System.out.println("Before : \n" + m1.pointerDebugString(p1));
          System.out.println("Needed : \n" + m2.pointerDebugString(p2));
        }

        p1 = m1.realloc(p1, newSize);

        if (DEBUG) {
          System.out.println("After : \n" + m1.pointerDebugString(p1));
        }

        m1.getInts(p1, 0, out, 0, out.length);
        assertArrayEquals("Size : " + size, data, out);
        assertTrue("Size : " + size, m1.maximumCapacityFor(p1) >= newSize);

        assertEquals("Size : " + size, m1.usedBlocks(), m2.usedBlocks());
        assertEquals("Size : " + size, m1.maxBlocks(), m2.maxBlocks());
        assertEquals("Size : " + size, m1.blockSize(), m2.blockSize());

        assertEquals("Error growing from " + size + " to " + newSize, m1.pointerDebugString(p1),
            m2.pointerDebugString(p2));

      } catch (AssertionError e) {
        throw new AssertionError("Error, n=" + n, e);
      } catch (OutOfMemoryException e) {
        // expected due to allocators overhead, just run till we find max
        // allocation size
        break;
      } finally {
        if (p1 != -1) {
          m1.free(p1);
        }
        if (p2 != -1) {
          m2.free(p2);
        }

        assertEquals(0, m1.usedBlocks());
        assertEquals(0, m2.usedBlocks());
      }
    }
  }

  /**
   * Full spectrum realloc test for decreasing memory size
   */
  @Test
  public void reallocRemoveSingleBlockTest() {
    int maxBlocks = m_numBlocks;
    int blockSize = m_blockSize;
    IMemAllocator m1 = create();
    IMemAllocator m2 = create();
    for (int n = 1; n < maxBlocks; n++) {
      try {
        int size = n * blockSize;
        int newSize = (n - 1) * blockSize;
        m1.setInitializer(new MemSetInitializer(-1));
        m1.setDebug(true);

        m2.setInitializer(new MemSetInitializer(-1));
        m2.setDebug(true);

        int data[] = new int[newSize];
        int out[] = new int[newSize];
        for (int j = 0; j < data.length; j++) {
          data[j] = j;
        }

        int p1 = m1.malloc(size);
        m1.setInts(p1, 0, data, 0, data.length);

        int p2 = m2.malloc(newSize);
        m2.setInts(p2, 0, data, 0, data.length);

        String s1 = m1.pointerDebugString(p1);
        String s2 = m2.pointerDebugString(p2);
        if (DEBUG) {
          System.out.println("Before : \n" + s1);
          System.out.println("Needed : \n" + s2);
        }

        p1 = m1.realloc(p1, newSize);

        s1 = m1.pointerDebugString(p1);
        if (DEBUG) {
          System.out.println("After : \n" + s1);
        }

        m1.getInts(p1, 0, out, 0, out.length);
        assertArrayEquals("Size : " + size, data, out);
        assertTrue("Size : " + size, m1.maximumCapacityFor(p1) >= newSize);

        assertEquals("Size : " + size, m1.usedBlocks(), m2.usedBlocks());
        assertEquals("Size : " + size, m1.maxBlocks(), m2.maxBlocks());
        assertEquals("Size : " + size, m1.blockSize(), m2.blockSize());

        assertEquals("Error shrinking from " + size + " to " + newSize, s1, s2);

        m1.free(p1);
        m2.free(p2);
        assertEquals(0, m1.usedBlocks());
        assertEquals(0, m2.usedBlocks());

      } catch (AssertionError e) {
        throw new AssertionError("Error, n=" + n, e);
      } catch (OutOfMemoryException e) {
        // expected due to allocators overhead, just run till we find max
        // allocation size
        m1.clear();
        m2.clear();
      }
    }
  }
}
