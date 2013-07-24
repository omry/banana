/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.map;

import net.yadan.banana.DebugLevel;
import net.yadan.banana.Formatter;
import net.yadan.banana.memory.Buffer;
import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IPrimitiveAccess;


/**
 * This usage example demonstrates using a map where the key is a variable
 * length structure In addition, it demonstrates how to use a Formatter to make
 * debugging the map easier
 * 
 * Created by : omry Date: 6/29/13
 */
public class FormattingMapElements {
  public static void main(String[] args) {
    /**
     * Lets say we want the following C like struct as the value of our map.
     * Note that this is variable length struct.
     * 
     * <pre>
     * struct person {
     *  int age
     *  struct name {
     *    int size
     *    char chars[]
     *  }
     * }
     * 
     *      
     * 
     * Since banana storage is always ints, a record for 13 year old Romeo would look like
     * 
     * ┌──0──╦──1──┬──2──┬──3──┬──4──┐
     * │     ║       NAME  STRING    │
     * │ AGE ╬─────╦─────────────────┤
     * │     ║SIZE ║      CHARS      │
     * ├─────╬─────╬─────┬─────┬─────┤
     * │ 13  ║  5  ║ R|o │ m|e │ o|  │
     * └─────╩─────╩─────┴─────┴─────┘
     * 
     * Note that each int contains two chars (Java chars are 16 bit values).
     * </pre>
     */
    final int AGE_OFFSET = 0;
    final int NAME_SIZE_OFFSET = 1;
    final int NAME_CHARS_OFFSET = 2;

    // initial number of blocks in the block allocator
    int maxBlocks = 100;
    // minimal size of data we will want to put into the map (in ints)
    int blockSize = 5;
    // how full should the map be before incresing size and rehashing?
    double loadFactor = 0.75;
    // if we run out of blocks in the underlying block allocator, grow it by
    // what factor?
    double growthFactor = 2.0;
    IHashMap map = new HashMap(maxBlocks, blockSize, growthFactor, loadFactor);
    
    long key = 1000;
    char name1[] = "Romeo".toCharArray();

    IBuffer romeo = new Buffer(10);
    romeo.setInt(AGE_OFFSET, 13);
    romeo.setInt(NAME_SIZE_OFFSET, name1.length);
    romeo.setChars(NAME_CHARS_OFFSET, name1, 0, name1.length);
    map.createRecord(key, romeo);

    long key2 = 1002;
    char name2[] = "Juliet".toCharArray();
    // this will be enough to hold the data
    int size = NAME_CHARS_OFFSET + name2.length / 2 + 1;
    // create empty record for size ints and get handle back
    int r = map.createRecord(key2, size);
    map.setInt(r, AGE_OFFSET, 13);
    map.setInt(r, NAME_SIZE_OFFSET, name2.length);
    map.setChars(r, NAME_CHARS_OFFSET, name2, 0, name2.length);

    // you can use a formatter to make the map easier to debug by using your own
    // logic to convert records to strings
    map.setFormatter(new Formatter() {
      @Override
      public String format(IPrimitiveAccess parent, int pointer) {
        int age = parent.getInt(pointer, AGE_OFFSET);
        int num = parent.getInt(pointer, NAME_SIZE_OFFSET);
        char chars[] = new char[num];
        parent.getChars(pointer, NAME_CHARS_OFFSET, chars, 0, num);
        return String.format("%s, age %d%s", new String(chars), age, age < 18 ? " (underage!)" : "");
      }
    });

    map.setDebug(DebugLevel.DEBUG_CONTENT);
    System.out.println(map);
    /**
     * Outputs :
     * 
     * <pre>
     * Map : net.yadan.banana.map.HashMap 2 / 100
     * 1000=Romeo, age 13 (underage!)
     * 1002=Juliet, age 13 (underage!)
     * </pre>
     */

  }
}

