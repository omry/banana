package net.yadan.banana.memory.malloc.chainedallocator;

import net.yadan.banana.memory.initializers.MemSetInitializer;
import net.yadan.banana.memory.malloc.AbstractOOMTest;
import net.yadan.banana.memory.malloc.ChainedAllocator;


public class ChainedOOMTest extends AbstractOOMTest {

  public ChainedOOMTest(int numBlocks, int blockSize,
      int allocationSize, boolean shouldOOM) {
    super(numBlocks, blockSize, allocationSize, shouldOOM);
  }

  @Override
  protected void init(int numBlocks, int blockSize) {
    m = new ChainedAllocator(numBlocks, blockSize);
    m.setInitializer(new MemSetInitializer(-1));
    m.setDebug(true);
  }
}
