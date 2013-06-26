package net.yadan.banana.memory.malloc.chainedallocator;

import net.yadan.banana.memory.initializers.MemSetInitializer;
import net.yadan.banana.memory.malloc.AbstractMemAllocatorTest;
import net.yadan.banana.memory.malloc.ChainedAllocator;


public class ChainedMemAllocatorTest extends AbstractMemAllocatorTest {

  public ChainedMemAllocatorTest(int numBlocks, int blockSize, int allocationSize) {
    super(numBlocks, blockSize, allocationSize);
  }

  @Override
  public void init(int numBlocks, int blockSize) {
    m = new ChainedAllocator(numBlocks, blockSize);
    m.setInitializer(new MemSetInitializer(-1));
    m.setDebug(true);
  }
}
