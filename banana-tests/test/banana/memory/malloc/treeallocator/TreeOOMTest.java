package banana.memory.malloc.treeallocator;

import banana.memory.initializers.MemSetInitializer;
import banana.memory.malloc.AbstractOOMTest;
import banana.memory.malloc.TreeAllocator;


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
