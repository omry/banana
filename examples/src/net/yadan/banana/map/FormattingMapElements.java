/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.map;

import net.yadan.banana.Formatter;
import net.yadan.banana.memory.Buffer;
import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IPrimitiveAccess;


/**
 * Created by : omry
 * Date: 6/29/13
 */
public class FormattingMapElements {
  public static void main(String[] args) {
    int maxBlocks = 100;        // initial number of blocks in the block allocator
    int blockSize = 5;          // minimal size of data we will want  to put into the map (in ints)
    double loadFactor = 0.75;   // how full should the map be before incresing size and rehashing?
    double growthFactor = 2.0;  // if we run out of blocks in the underlying block allocator, grow it by what factor?
    IHashMap map = new HashMap(maxBlocks, blockSize, growthFactor, loadFactor);
    
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
     * Since banana storage is always ints, a record for 18 year old Jonathan would look like:
     * 
     * ┌──0──╦──1──┬──2──┬──3──┬──4──┬──5──┐
     * │     ║       NAME  STRING          │
     * │ AGE ╬─────╦───────────────────────┤
     * │     ║SIZE ║        CHARS          │
     * ├─────╬─────╬─────┬─────┬─────┬─────┤
     * │ 18  ║  8  ║  Jo │ na  │  th │ an  │
     * └─────╩─────╩─────┴─────┴─────┴─────┘
     * 
     * Note that each int contains two chars (Java chars are 16 bit values).
     * </pre>
     */
    final int AGE_OFFSET = 0;
    final int NAME_SIZE_OFFSET = 1;
    final int NAME_CHARS_OFFSET = 2;
    IBuffer person = new Buffer(6);

    person.setInt(AGE_OFFSET, 18);
    person.setInt(NAME_SIZE_OFFSET, 8);
    person.setChars(NAME_CHARS_OFFSET, "Jonathan".toCharArray());

  }

  static class PersonFormatter implements Formatter {

    @Override
    public String format(IPrimitiveAccess parent, int pointer) {
      return "";
      // return String.foramt("[%s (age=%d)]");
    }
  }
}

