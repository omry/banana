package net.yadan.banana.memory.malloc.chainedallocator;

import net.yadan.banana.memory.malloc.AbstractComputeMemoryUsageTest;
import net.yadan.banana.memory.malloc.ChainedAllocator;


public class ChainedComputeMemoryUsageTest extends AbstractComputeMemoryUsageTest {

  public ChainedComputeMemoryUsageTest(int maxBlocks, int blockSize, int allocationSize) {
    super(maxBlocks, blockSize, allocationSize);
  }

  @Override
  public void init(int maxBlocks, int blockSize) {
    m = new ChainedAllocator(maxBlocks, blockSize);
  }

  @Override
  public int computeExpectedCapacityFor(int size) {
    if (size <= m.blockSize()) {
      return m.blockSize();
    } else {
      int sizePerBlock = m.blockSize() - 1;
      return (1 + (size - 1) / sizePerBlock) * sizePerBlock;
    }
  }
}
