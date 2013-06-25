package banana.memory.malloc.treeallocator;

import banana.memory.IMemAllocator;
import banana.memory.initializers.MemSetInitializer;
import banana.memory.malloc.AbstractReallocTest;
import banana.memory.malloc.TreeAllocator;


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
