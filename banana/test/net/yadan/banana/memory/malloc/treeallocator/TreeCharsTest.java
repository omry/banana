package net.yadan.banana.memory.malloc.treeallocator;

import net.yadan.banana.memory.initializers.MemSetInitializer;
import net.yadan.banana.memory.malloc.AbstractCharsTest;
import net.yadan.banana.memory.malloc.TreeAllocator;


public class TreeCharsTest extends AbstractCharsTest {

  public TreeCharsTest(int numBlocks, int blockSize, int allocationSize) {
    super(numBlocks, blockSize, allocationSize);
  }

  @Override
  public void init(int numBlocks, int blockSize) {
    m = new TreeAllocator(numBlocks, blockSize);
    m.setInitializer(new MemSetInitializer(-1));
    m.setDebug(true);
  }
}
