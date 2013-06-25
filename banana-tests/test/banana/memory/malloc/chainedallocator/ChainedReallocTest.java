package banana.memory.malloc.chainedallocator;

import banana.memory.IMemAllocator;
import banana.memory.initializers.MemSetInitializer;
import banana.memory.malloc.AbstractReallocTest;
import banana.memory.malloc.ChainedAllocator;


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
