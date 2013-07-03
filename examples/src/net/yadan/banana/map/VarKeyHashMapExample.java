package net.yadan.banana.map;

import net.yadan.banana.memory.Buffer;
import net.yadan.banana.memory.IBuffer;

/**
 * Created by : omry
 * Date: 7/2/13
 */
public class VarKeyHashMapExample {
  public static void main(String[] args) {
    int maxBlocks = 100;        // initial number of blocks in the block allocator
    int blockSize = 5;          // minimal size of data we will want  to put into the map (in ints)
    double loadFactor = 0.75;   // how full should the map be before incresing size and rehashing?
    double growthFactor = 2.0;  // if we run out of blocks in the underlying block allocator, grow it by what factor?
    IVarKeyHashMap map = new VarKeyHashMap(maxBlocks, blockSize, growthFactor, loadFactor);

    // a key object we will reuse.
    // this is a typical pattern because we want to avoid unneeded objects creation.
    IBuffer key = new Buffer(10);
    key.appendLong(1001);
    // create a new record, key is the long 1001 . record data size is 5.
    int r1 = map.createRecord(key, 5);
    // set some data into the record
    map.setInt(r1, 0, 99);
    map.setLong(r1, 1, Long.MIN_VALUE);
    map.setLong(r1, 3, Long.MAX_VALUE);

    // will allow to to reuse the key
    key.reset();
    // append some chars to the reused key
    key.appendChars("Hello World".toCharArray());
    // create a record that can hold 30 ints of data. actual memory will span multiple blocks in the MemAllocator
    int r2 = map.createRecord(key, 30);
    // fill it up with useful data
    for (int i = 0; i < 30; i++) {
      int data = i * i * i;
      int offset = i;
      map.setInt(r2, offset, data);
    }

    // find the record id for the key long 1001
    key.reset();
    key.appendLong(1001);
    int r3 = map.findRecord(key);
    // which incidently should be r1
    if (r3 != r1) throw new IllegalStateException("Bug!");

    // read the data from r3 and make sure it's what we put in there
    if (99 != map.getInt(r3, 0)) throw new IllegalStateException("Bug!");
    if (Long.MIN_VALUE != map.getLong(r3, 1)) throw new IllegalStateException("Bug!");
    if (Long.MAX_VALUE != map.getLong(r3, 3)) throw new IllegalStateException("Bug!");

    // remove the record with key 1001
    map.remove(key);

    // find the 1001 record again, since it's not there we will get -1 as the record id
    int r4 = map.findRecord(key);
    if (r4 != -1) throw new IllegalStateException("Bug!");

    // on the other hand Hello World is still in the map
    key.reset();
    key.appendChars("Hello World".toCharArray());
    if (!map.containsKey(key)) throw new IllegalStateException("Bug!");

    // record with room for 10 ints
    IBuffer value = new Buffer(10);
    char[] chars = "Hello world".toCharArray();
    value.appendChars(chars);

    // create  a record with that value
    key.reset();
    key.appendLong(1003);
    map.createRecord(key, value);

    // clear the value for reuse
    value.reset();
    value.setInts(0, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, 0, 10);

    key.reset();
    key.appendLong(1004);
    map.createRecord(key, value);


    map.visitRecords(new VarKeyHashMapVisitor() {
      @Override
      public void begin(IVarKeyHashMap map) {
        // called before visiting the first record
      }

      @Override
      public void visit(IVarKeyHashMap map, int keyPtr, int valuePtr, long num, long total) {
        // called when visiting each record
        // key is the record key
        // record_id is the direct id (can be used to set/get data
      }

      @Override
      public void end(IVarKeyHashMap map) {
        // called  after visiting the last record
      }
    });

    // remove all records from the map, and free their memory.
    map.clear();
  }
}
