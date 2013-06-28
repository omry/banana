package net.yadan.banana.memory.malloc.chainedallocator;

import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.initializers.MemSetInitializer;
import net.yadan.banana.memory.malloc.AbstractReallocTest;
import net.yadan.banana.memory.malloc.ChainedAllocator;


public class ChainedReallocTest extends AbstractReallocTest {

  public ChainedReallocTest(int numBlocks, int blockSize) {
    super(numBlocks, blockSize);
  }

  @Override
  public IMemAllocator createImpl(int numBlocks, int blockSize) {
    ChainedAllocator m = new ChainedAllocator(numBlocks, blockSize);
    m.setInitializer(new MemSetInitializer(-1));
    m.setDebug(true);
    return m;
  }
}
