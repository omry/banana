package banana.memory.block;

import banana.memory.IBlockAllocator;
import banana.memory.MemInitializer;
import banana.memory.block.BlockAllocator;


public class BlockAllocatorTest extends AbstractBlockAllocatorTest {
  @Override
  public IBlockAllocator create(int numBlocks, int blockSize, double growthFactor,
      MemInitializer initializer) {
    BlockAllocator m = new BlockAllocator(numBlocks, blockSize, growthFactor, initializer);
    m.setDebug(true);
    return m;
  };
}
