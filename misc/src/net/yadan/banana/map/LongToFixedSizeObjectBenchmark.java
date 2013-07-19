package net.yadan.banana.map;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import net.yadan.utils.Util;

public class LongToFixedSizeObjectBenchmark {

  public static void main(String[] args) {
    int max = 25 * 1000 * 1000;
    int keys[] = new int[max];
    for (int i = 0; i < max; i++) {
      keys[i] = i;
    }
    System.out.print("Shuffling...");
    Util.shuffleArray(keys);
    System.out.println("Done");

    long t = System.currentTimeMillis();
    testBanana(keys);
    System.out.println("Banana elapsed : " + (System.currentTimeMillis() - t) + " ms");

    t = System.currentTimeMillis();
    testJava(keys);
    System.out.println("Java elapsed : " + (System.currentTimeMillis() - t) + " ms");
  }


  /**
   * Raw banana offsets (in ints) for the Java Object representation
   *
   *     name             : ints size : OFFSET
   *     -----------------------------------
   *     long session_id  : 2         : 0
   *     byte ipv6[16]    : 4         : 2
   *     short port;      : 0.5       : 6 (upper short)
   *     short type;      : 0.5       : 6 (lower short)
   *     ----------------------
   *     total ints       : 7
   */

  public static int RECORD_SIZE       = 7;
  public static int SESSION_ID_OFFSET = 0;
  public static int IPV6_OFFSET = 2;
  public static int PORT_OFFSET = 6; // upper short
  public static int TYPE_OFFSET = 6; // lower short

  public static void testBanana(int keys[]) {
    int num = keys.length;

    IHashMap h = new HashMap(num, RECORD_SIZE, 0, 1);
    for (int i = 0; i < keys.length; i++) {
      if (i % 3000000 == 0) {
        System.out.println("Inserted " + i);
      }
      int key = keys[i];

      int r = h.createRecord(key, RECORD_SIZE);
      h.setLong(r, SESSION_ID_OFFSET, key);
      h.setInt(r, IPV6_OFFSET + 0, 1);
      h.setInt(r, IPV6_OFFSET + 1, 2);
      h.setInt(r, IPV6_OFFSET + 2, 3);
      h.setInt(r, IPV6_OFFSET + 3, 4);
      h.setUpperShort(r, PORT_OFFSET, 9999);
      h.setLowerShort(r, TYPE_OFFSET, 0);
    }

    for (int i = 0; i < keys.length; i++) {
      int key = keys[i];
      int r = h.findRecord(key);
      if (h.getLong(r, SESSION_ID_OFFSET) != key) {
        throw new RuntimeException("failed");
      }
    }

    System.gc();
    System.out.println(h.size());
    MemoryMXBean mx = ManagementFactory.getMemoryMXBean();
    MemoryUsage usage = mx.getHeapMemoryUsage();
    System.out.println("Memory used by banana hashmap : " + usage.getUsed());
  }



  /**
   * Java object representing some fixed some data structure of size
   *     name             : size in bytes
   *     -----------------:--------------
   *     long session_id  : 8
   *     byte ipv6[16]    : 16
   *     short port;      : 2
   *     short type;      : 2
   *     ----------------------
   *     total bytes      | 28
   */
  static class JavaObject {
    long session_id;
    int ipv6[] = new int[4];
    short port;
    short type;

    JavaObject(long sid) {
      session_id = sid;
    }
  }

  public static void testJava(int keys[]) {

    java.util.HashMap<Long, JavaObject> h = new java.util.HashMap<Long, JavaObject>(keys.length, 1.0f);
    for (int i = 0; i < keys.length; i++) {
      if (i % 3000000 == 0) {
        System.out.println("Inserted " + i);
      }
      long key = keys[i];
      JavaObject o = new JavaObject(key);
      o.ipv6[0] = 1;
      o.ipv6[1] = 2;
      o.ipv6[2] = 3;
      o.ipv6[3] = 4;
      o.port = 9999;
      o.type = 0;
      h.put(key, o);
    }

    for (int i = 0; i < keys.length; i++) {
      long key = keys[i];
      JavaObject r = h.get(key);
      if (r.session_id != key)
        throw new RuntimeException("failed");
    }

    System.gc();
    System.out.println(h.size());
    MemoryMXBean mx = ManagementFactory.getMemoryMXBean();
    MemoryUsage usage = mx.getHeapMemoryUsage();
    System.out.println("Memory used by java hashmap : " + usage.getUsed());
  }
}
