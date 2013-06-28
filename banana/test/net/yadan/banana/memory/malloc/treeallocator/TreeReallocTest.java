package net.yadan.banana.memory.malloc.treeallocator;

import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.initializers.MemSetInitializer;
import net.yadan.banana.memory.malloc.AbstractReallocTest;
import net.yadan.banana.memory.malloc.TreeAllocator;


public class TreeReallocTest extends AbstractReallocTest {


  public TreeReallocTest(int numBlocks, int blockSize) {
    super(numBlocks, blockSize);
  }

  @Override
  public IMemAllocator createImpl(int numBlocks, int blockSize) {
    TreeAllocator m = new TreeAllocator(numBlocks, blockSize);
    m.setInitializer(new MemSetInitializer(-1));
    m.setDebug(true);
    return m;
  }

}
