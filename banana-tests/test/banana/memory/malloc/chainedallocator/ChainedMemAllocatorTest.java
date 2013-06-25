package banana.memory.malloc.chainedallocator;

import banana.memory.initializers.MemSetInitializer;
import banana.memory.malloc.AbstractMemAllocatorTest;
import banana.memory.malloc.ChainedAllocator;


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
