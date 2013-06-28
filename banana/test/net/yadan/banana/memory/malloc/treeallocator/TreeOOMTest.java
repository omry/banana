package net.yadan.banana.memory.malloc.treeallocator;

import net.yadan.banana.memory.initializers.MemSetInitializer;
import net.yadan.banana.memory.malloc.AbstractOOMTest;
import net.yadan.banana.memory.malloc.TreeAllocator;


public class TreeOOMTest extends AbstractOOMTest {

  public TreeOOMTest(int numBlocks, int blockSize, int allocationSize, boolean shouldOOM) {
    super(numBlocks, blockSize, allocationSize, shouldOOM);
  }

  @Override
  protected void init(int numBlocks, int blockSize) {
    m = new TreeAllocator(numBlocks, blockSize);
    m.setInitializer(new MemSetInitializer(-1));
    m.setDebug(true);
  }
}
