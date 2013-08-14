/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.list;

import net.yadan.banana.DebugLevel;
import net.yadan.banana.Formatter;
import net.yadan.banana.memory.IPrimitiveAccess;

/**
 * This example demonstrates how to use the Formatter to get easier to debug
 * list representation
 * 
 * @author omry
 * 
 */
public class FormattingListElements {

  public static void main(String[] args) {

    int maxRecords = 100;
    int blockSize = 5;
    double growthFactor = 2;
    LinkedList list = new LinkedList(maxRecords, blockSize, growthFactor);

    /**
     * <pre>
     * Lets say for this example our list records would looks like this in a C like language:
     * (Remember our underlying data type is int)
     * struct {
     *  int num
     *  struct description {
     *    int length
     *    int chars[]
     *  }
     * }
     * 
     * Note that this is a variable length record
     * 
     * Offset   | Data 
     * ---------+------ 
     * 0        | Number of Bananas 
     * 1        | Length of description (in chars) 
     * 2...     | Actual description (each int holds two chars except maybe the last).
     * 
     * We would like the formatting of each record to look something like: 
     * #22 Bananas of type Big-Yellow
     * </pre>
     */

    // Data type offsets as described above for clarity
    final int NUM_OFFSET = 0;
    final int NAME_SIZE_OFFSET = 1;
    final int NAME_OFFSET = 2;

    // lets prepare 4 bananas of type Small=Green
    char[] chars = "Small-Green".toCharArray();

    // make it the head head of the list
    int r = list.insertHead(NAME_OFFSET + (int) Math.ceil(chars.length / 2.0));
    list.setInt(r, NUM_OFFSET, 4);
    list.setInt(r, NAME_SIZE_OFFSET, chars.length);
    list.setChars(r, NAME_OFFSET, chars, 0, chars.length);

    // lets prepare 2 bananas of type Big-Yellow-Of-The-Best-Kind
    // make it the new head of the list
    chars = "Big-Yellow-Of-The-Best-Kind".toCharArray();
    r = list.insertHead(NAME_OFFSET + (int) Math.ceil(chars.length / 2.0));
    list.setInt(r, NUM_OFFSET, 2);
    list.setInt(r, NAME_SIZE_OFFSET, chars.length);
    list.setChars(r, NAME_OFFSET, chars, 0, chars.length);

    list.setDebug(DebugLevel.DEBUG_CONTENT);
    System.out.println(list.toString());

    /**
     * Outputs the not very useful:
     * 
     * <pre>
     * LinkedList (2 records)
     * [2,27,4325481,6750253,5832805,7077996,7274615,2949199,6684717,5505128,6619181,4325477,7536756,2949195,6881390,6553600,0] -> [4,11,5439597,6357100,7077933,4653170,6619237,7208960,0,0,0]
     * </pre>
     * 
     * Lets try to make the debug output friendlier
     * 
     * <pre>
     * LinkedList (2 records)
     * [#2 Bananas of type Big-Yellow-Of-The-Best-Kind] -> [#4 Bananas of type Small-Green]
     * </pre>
     */

    list.setFormatter(new Formatter() {

      @Override
      public String format(IPrimitiveAccess parent, int pointer) {
        int num = parent.getInt(pointer, NUM_OFFSET);
        int nameCharsLength = parent.getInt(pointer, NAME_SIZE_OFFSET);
        // we could potentially reuse this chars array to improve performance
        char chars[] = new char[nameCharsLength];
        parent.getChars(pointer, NAME_OFFSET, chars, 0, chars.length);
        return String.format("[#%d Bananas of type %s]", num, new String(chars));
      }
    });
    System.out.println(list.toString());
  }
}
