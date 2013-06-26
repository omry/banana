package net.yadan.banana.memory.malloc.treeallocator;

import net.yadan.banana.memory.initializers.MemSetInitializer;
import net.yadan.banana.memory.malloc.AbstractMemAllocatorTest;
import net.yadan.banana.memory.malloc.TreeAllocator;


public class TreeMemAllocatorTest extends AbstractMemAllocatorTest {

  public TreeMemAllocatorTest(int numBlocks, int blockSize, int allocationSize) {
    super(numBlocks, blockSize, allocationSize);
  }

  @Override
  public void init(int numBlocks, int blockSize) {
    m = new TreeAllocator(numBlocks, blockSize);
    m.setInitializer(new MemSetInitializer(-1));
    m.setDebug(true);
  }
}
