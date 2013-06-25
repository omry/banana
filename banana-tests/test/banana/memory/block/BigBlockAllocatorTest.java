package banana.memory.block;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import banana.memory.IBlockAllocator;
import banana.memory.MemInitializer;
import banana.memory.block.BigBlockAllocator;
import banana.memory.initializers.MemSetInitializer;


public class BigBlockAllocatorTest extends AbstractBlockAllocatorTest {
  private static final int MAX_INTS_PER_ARRAY = 20;

  static {
    System.setProperty("BigIntBlockAllocator.MAX_INTS_PER_ARRAY", "" + MAX_INTS_PER_ARRAY);
  }

  @Override
  public IBlockAllocator create(int numBlocks, int blockSize, double growthFactor,
      MemInitializer initializer) {

    BigBlockAllocator m = new BigBlockAllocator(numBlocks, blockSize, growthFactor, initializer);
    m.setDebug(true);
    return m;

  };

  @Test
  public void testGrowthInsideLastBlock() {

    int initialMax = 10;
    int pointers[] = new int[initialMax];
    double growthFactor = 2.0;

    a = create(initialMax, 2, growthFactor, new MemSetInitializer(-1));
    for (int i = 0; i < initialMax; i++) {
      int p = a.malloc();
      a.setLong(p, 0, i);
      pointers[i] = p;
    }

    assertEquals(0, a.freeBlocks());
    assertEquals(initialMax, a.maxBlocks());

    a.malloc(); // grow

    assertEquals((int) (initialMax * growthFactor), a.maxBlocks());
    assertEquals(initialMax + 1, a.usedBlocks());
    assertEquals(a.maxBlocks() - a.usedBlocks(), a.freeBlocks());

    for (int i = 0; i < initialMax; i++) {
      assertEquals(i, a.getLong(pointers[i], 0));
    }

    a.clear();
  }

  @Test
  public void testGrowthOverBlocks() {

    int initialMax = 12;
    int pointers[] = new int[initialMax];
    double growthFactor = 2.0;
    int blockSize = 1;
    a = create(initialMax, blockSize, growthFactor, null);
    assertEquals(4 * (initialMax + 1) * blockSize, a.computeMemoryUsage());

    for (int i = 0; i < initialMax; i++) {
      int p = a.malloc();
      a.setInt(p, 0, i);
      pointers[i] = p;
    }

    assertEquals(0, a.freeBlocks());
    assertEquals(initialMax, a.maxBlocks());
    assertEquals(initialMax, a.usedBlocks());

    a.malloc(); // grow * 2

    assertEquals((int) (initialMax * growthFactor), a.maxBlocks());
    assertEquals(initialMax + 1, a.usedBlocks());
    assertEquals(a.maxBlocks() - a.usedBlocks(), a.freeBlocks());
    // + 1 for reserved block
    assertEquals(4 * (a.maxBlocks() + 1), a.computeMemoryUsage());

    for (int i = 0; i < initialMax; i++) {
      assertEquals(i, a.getInt(pointers[i], 0));
    }

    a.clear();
  }

  @Test
  public void testGrowthOverBlocksWithBigGrowth() {

    int initialMax = 12;
    int pointers[] = new int[initialMax];
    int growthFactor = 5;

    a = create(initialMax, 1, growthFactor, null);
    for (int i = 0; i < initialMax; i++) {
      int p = a.malloc();
      a.setInt(p, 0, i);
      pointers[i] = p;
    }

    assertEquals(initialMax, a.usedBlocks());
    assertEquals(0, a.freeBlocks());
    assertEquals(initialMax, a.maxBlocks());

    a.malloc(); // grow
    assertEquals(initialMax + 1, a.usedBlocks());
    assertEquals(initialMax * 5, a.maxBlocks());
    // +1 block for reserved
    assertEquals(4 * (1 + a.maxBlocks()), a.computeMemoryUsage());

    for (int i = 0; i < initialMax; i++) {
      assertEquals(i, a.getInt(pointers[i], 0));
    }

    a.clear();
  }

  @Test
  public void testMultiArrayIntAccess() {
    a = create(60, 1, 2.0, null);
    int pointers[] = new int[a.maxBlocks()];
    for (int i = 0; i < a.maxBlocks(); i++) {
      int p = a.malloc();
      pointers[i] = p;
      a.setInt(p, 0, i);
      assertEquals(i, a.getInt(p, 0));
    }

    for (int i = 0; i < a.maxBlocks(); i++) {
      a.free(pointers[i]);
    }
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
