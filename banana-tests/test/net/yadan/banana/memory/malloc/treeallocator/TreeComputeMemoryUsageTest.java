package net.yadan.banana.memory.malloc.treeallocator;

import net.yadan.banana.memory.malloc.AbstractComputeMemoryUsageTest;
import net.yadan.banana.memory.malloc.TreeAllocator;


public class TreeComputeMemoryUsageTest extends AbstractComputeMemoryUsageTest {

  public TreeComputeMemoryUsageTest(int maxBlocks, int blockSize, int allocationSize) {
    super(maxBlocks, blockSize, allocationSize);
  }

  @Override
  public void init(int maxBlocks, int blockSize) {
    m = new TreeAllocator(maxBlocks, blockSize);
  }

  @Override
  public int computeExpectedCapacityFor(int size) {
    return (1 + ((m_allocationSize - 1) / m.blockSize())) * m.blockSize();
  }
}
