package net.yadan.banana.memory.block;

import net.yadan.banana.memory.IBlockAllocator;
import net.yadan.banana.memory.MemInitializer;


public class BlockAllocatorTest extends AbstractBlockAllocatorTest {
  @Override
  public IBlockAllocator create(int numBlocks, int blockSize, double growthFactor,
      MemInitializer initializer) {
    BlockAllocator m = new BlockAllocator(numBlocks, blockSize, growthFactor, initializer);
    m.setDebug(true);
    return m;
  };
}
