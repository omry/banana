package banana.memory.malloc.chainedallocator;

import banana.memory.initializers.MemSetInitializer;
import banana.memory.malloc.AbstractOOMTest;
import banana.memory.malloc.ChainedAllocator;


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
