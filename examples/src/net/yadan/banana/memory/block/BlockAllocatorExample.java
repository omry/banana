/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory.block;

import net.yadan.banana.memory.IBlockAllocator;
import net.yadan.banana.memory.initializers.MemSetInitializer;

public class BlockAllocatorExample {
  public static void main(String[] args) {

    int maxBlocks = 100;
    int blockSize = 10;
    double growthFactor = 2;

    // This will allocate a new BlockAllocator with 100 blocks of 10 ints
    // Growth factor of 2 means it will double in size if we ever try to use
    // more blocks than it's current maxBlocks at the same time
    IBlockAllocator allocator = new BlockAllocator(maxBlocks, blockSize, growthFactor);

    // allocate a block
    int pointer1 = allocator.malloc();

    // fill up the block with some ints
    for (int offset = 0; offset < blockSize; offset++) {
      int data = offset * 2;
      allocator.setInt(pointer1, offset, data);
    }

    // set a long at offset 5, will use two ints
    allocator.setLong(pointer1, 5, -1);

    System.out.println("First block:");
    printBlock(allocator, pointer1);


    // set the block initializer, newly allocator blocks will have all their ints set to 9
    allocator.setInitializer(new MemSetInitializer(9));

    int pointer2 = allocator.malloc();
    // copy an int array into the block
    int src_data[] = {0,2,4,6,8};
    int dst_offset_in_record = 0;
    int src_pos = 0;
    int length = src_data.length;
    allocator.setInts(pointer2, dst_offset_in_record, src_data, src_pos, length);

    System.out.println("Second block:");
    printBlock(allocator, pointer2);


    // once we are done with the memory, we need to free it to avoid a leak
    // in this case it will not actually make a difference because the whole allocator will soon
    // garbage collector, but it's the right thing to do.
    allocator.free(pointer1);
    allocator.free(pointer2);

//    OUTPUT:
//    First block:
//    [0,2,4,6,8,-1,-1,14,16,18]
//    Second block:
//    [0,2,4,6,8,9,9,9,9,9]
  }

  protected static void printBlock(IBlockAllocator allocator, int pointer) {
    System.out.print("[");
    for (int offset = 0; offset < allocator.blockSize(); offset++) {
      System.out.print(allocator.getInt(pointer, offset));
      if (offset + 1 < allocator.blockSize())
        System.out.print(",");
    }
    System.out.println("]");
  }
}
