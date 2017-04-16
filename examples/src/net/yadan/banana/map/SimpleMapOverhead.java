//package net.yadan.banana.map;
//
//import net.yadan.banana.memory.IMemAllocator;
//
//
//public class SimpleMapOverhead {
//  public static void main(String[] args) {
//    int maxBlocks = (int) (1 * 1e6);        // initial number of blocks in the block allocator
//    IMemAllocator allocator = new IntAllocator();
//    int blockSize = 1;          // minimal size of data we will want  to put into the map (in ints)
//    double loadFactor = 0.75;   // how full should the map be before incresing size and rehashing?
//    double growthFactor = 2.0;  // if we run out of blocks in the underlying block allocator, grow it by what factor?
//    
//    IHashMap map = new HashMap(maxBlocks, blockSize, growthFactor, loadFactor);
//    for(int i=0;i<maxBlocks;i++) {
//      int ptr = map.createRecord(i, 10);
//      map.setLong(ptr, 0, i);
//    }
//    
//    System.out.println("Usage " + map.computeMemoryUsage());
//  }
//}
