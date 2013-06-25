package banana.memory.malloc;

import banana.memory.IMemAllocator;

public class MemoryAllocatorsExample {

  static final int BLOCK_SIZE = 10;
  static final int MAX_BLOCKS = 100;
  static final double GROWTH_FACTOR = 2;

  public static void main(String[] args) {

    IMemAllocator allocator1 = new ChainedAllocator(MAX_BLOCKS, BLOCK_SIZE, GROWTH_FACTOR);
    testAllocator(allocator1);

    IMemAllocator allocator2 = new TreeAllocator(MAX_BLOCKS, BLOCK_SIZE, GROWTH_FACTOR);
    testAllocator(allocator2);

    IMemAllocator allocator3 = new MultiSizeAllocator(MAX_BLOCKS, new int[]{10,25,50}, GROWTH_FACTOR);
    testAllocator(allocator3);
  }

  protected static void testAllocator(IMemAllocator allocator) {
    String name = allocator.getClass().getSimpleName();

    // allocate a pointer of block size, this will allocate a single block
    // with all the data accessible
    int pointer1 = allocator.malloc(BLOCK_SIZE);

    // fill up the block with some ints
    for (int offset = 0; offset < BLOCK_SIZE; offset++) {
      int data = offset * 2;
      allocator.setInt(pointer1, offset, data);
    }

    System.out.println(name + " : First allocation, pointer can support "
        + allocator.maximumCapacityFor(pointer1) + " ints");
    System.out.println(name + " : " + allocator.pointerDebugString(pointer1));

    // allocate memory of size 20, even though underlying blocks are of size 10.
    int pointer2 = allocator.malloc(20);

    // fill up the block with some ints
    for (int offset = 0; offset < 20; offset++) {
      int data = offset * 2;
      allocator.setInt(pointer2, offset, data);
    }

    // Prints the debug representation of the pointer, which is in fact a linked
    // list
    System.out.println(name + " : Second allocation, pointer can support "
        + allocator.maximumCapacityFor(pointer2) + " ints");
    System.out.println(name + " : " + allocator.pointerDebugString(pointer2));

    allocator.free(pointer1);
    allocator.free(pointer2);

  }
}
