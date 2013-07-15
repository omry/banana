package net.yadan.banana.list;

import net.yadan.banana.DebugLevel;
import net.yadan.banana.Formatter;
import net.yadan.banana.memory.Buffer;
import net.yadan.banana.memory.IBuffer;
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

    IBuffer link = new Buffer(20);
    // lets prepare 4 bananas of type Small=Green
    char[] chars = "Small-Green".toCharArray();
    link.setInt(NUM_OFFSET, 4);
    link.setInt(NAME_SIZE_OFFSET, chars.length);
    link.setChars(NAME_OFFSET, chars);

    // make it the head head of the list
    list.insertHead(link);

    // lets prepare 2 bananas of type Big-Yellow-Of-The-Best-Kind
    link.reset(); // reset link so we can reuse it
    chars = "Big-Yellow-Of-The-Best-Kind".toCharArray();
    link.setInt(NUM_OFFSET, 2);
    link.setInt(NAME_SIZE_OFFSET, chars.length);
    link.setChars(NAME_OFFSET, chars);

    // make it the new head of the list
    list.insertHead(link);

    list.setDebug(DebugLevel.DEBUG_CONTENT);
    System.out.println(list.toString());

    /**
     * Outputs the not very useful: LinkedList (2 records)
     * 
     * <pre>
     * LinkedList (2 records)
     * [2,1b,420069,67002d,590065,6c006c,6f0077,2d004f,66002d,540068,65002d,420065,730074,2d004b,69006e,640000,0] -> [4,b,53006d,61006c,6c002d,470072,650065,6e0000,0,0,0]
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

        // quick and efficient ceil(a/b) to get number of ints rounded up.
        int nameLen = 1 + (nameCharsLength - 1) / 2;

        // temp buffer to hold name
        Buffer name = new Buffer(nameLen);
        parent.getBuffer(pointer, NAME_OFFSET, name, nameLen);
        char chars[] = new char[nameCharsLength];
        name.getChars(0, chars, 0, chars.length);
        return String.format("[#%d Bananas of type %s]", num, new String(chars));
      }
    });
    System.out.println(list.toString());
  }
}
