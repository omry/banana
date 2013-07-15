/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.map;

import net.yadan.banana.memory.Buffer;
import net.yadan.banana.memory.IBuffer;

/**
 * Created by : omry
 * Date: 6/29/13
 */
public class HashMapExample {
  public static void main(String[] args) {
    int maxBlocks = 100;        // initial number of blocks in the block allocator
    int blockSize = 5;          // minimal size of data we will want  to put into the map (in ints)
    double loadFactor = 0.75;   // how full should the map be before incresing size and rehashing?
    double growthFactor = 2.0;  // if we run out of blocks in the underlying block allocator, grow it by what factor?
    IHashMap map = new HashMap(maxBlocks, blockSize, growthFactor, loadFactor);
    // create a new record, key is 1001. record data size is 5.
    int r1 = map.createRecord(1001, 5);
    // set some data into the record
    map.setInt(r1, 0, 99);
    map.setLong(r1, 1, Long.MIN_VALUE);
    map.setLong(r1, 3, Long.MAX_VALUE);

    // create a record that can hold 30 ints of data. actual memory will span multiple blocks in the MemAllocator
    int r2 = map.createRecord(1002, 30);
    // fill it up with useful data
    for (int i = 0; i < 30; i++) {
      int data = i * i * i;
      int offset = i;
      map.setInt(r2, offset, data);
    }

    // find the record id for the key 1001
    int r3 = map.findRecord(1001);
    // which incidently should be r1
    if (r3 != r1) throw new IllegalStateException("Bug!");

    // read the data from r3 and make sure it's what we put in there
    if (99 != map.getInt(r3, 0)) throw new IllegalStateException("Bug!");
    if (Long.MIN_VALUE != map.getLong(r3, 1)) throw new IllegalStateException("Bug!");
    if (Long.MAX_VALUE != map.getLong(r3, 3)) throw new IllegalStateException("Bug!");

    // remove the record with key 1001
    map.remove(1001);

    // find the 1001 record again, since it's not there we will get -1 as the record id
    int r4 = map.findRecord(1001);
    if (r4 != -1) throw new IllegalStateException("Bug!");

    // on the other hand 1002 is still in the map
    if (!map.containsKey(1002)) throw new IllegalStateException("Bug!");

    // record with room for 10 ints
    IBuffer value = new Buffer(10);
    char[] chars = "Hello world".toCharArray();
    value.appendChars(chars);

    // create  a record with that value
    map.createRecord(1003, value);

    // clear the value for reuse
    value.reset();
    value.setInts(0, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, 0, 10);
    map.createRecord(1004, value);


    map.visitRecords(new HashMapVisitor() {
      @Override
      public void begin(IHashMap map) {
        // called before visiting the first record
      }

      @Override
      public void visit(IHashMap map, long key, int record_id, long num, long total) {
        // called when visiting each record
        // key is the record key
        // record_id is the direct id (can be used to set/get data
      }

      @Override
      public void end(IHashMap map) {
        // called  after visiting the last record
      }
    });

    // remove all records from the map, and free their memory.
    map.clear();
  }
}
