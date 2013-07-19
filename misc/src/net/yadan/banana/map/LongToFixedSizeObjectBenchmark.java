package net.yadan.banana.map;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import net.yadan.utils.Util;

public class LongToFixedSizeObjectBenchmark {

  private static float banana_insert_rate;
  private static float fastutil_insert_rate;
  private static long banana_used;
  private static long java_used;
  private static long fastutil_used;
  private static float java_insert_rate;

  public static void main(String[] args) {
    int MILLION = 1000 * 1000;
    int max = 25 * MILLION;
    int keys[] = new int[max];
    for (int i = 0; i < max; i++) {
      keys[i] = i;
    }
    System.out.print("Shuffling...");
    Util.shuffleArray(keys);
    System.out.println("Done");
    long t, e;

    System.out.println(max / MILLION + " million items");
    t = System.currentTimeMillis();
    testBanana(keys);
    e = System.currentTimeMillis() - t;
    banana_insert_rate = max / ((float) MILLION) / (e / 1000f);
    System.out.println(String.format("%s : %f million/sec,  %d MB used", "Banana", banana_insert_rate, banana_used));

    t = System.currentTimeMillis();
    testFastUtil(keys);
    e = System.currentTimeMillis() - t;
    fastutil_insert_rate = max / ((float) MILLION) / (e / 1000f);
    System.out.println(String.format("%s : %f million/sec,  %d MB used", "FastUtil", fastutil_insert_rate,
        fastutil_used));

    t = System.currentTimeMillis();
    testJava(keys);
    e = System.currentTimeMillis() - t;
    java_insert_rate = max / ((float) MILLION) / (e / 1000f);
    System.out.println(String.format("%s : %f million/sec,  %d MB used", "Java", java_insert_rate, java_used));

    System.out.println(String.format("%.0f\t%.1f\t%.1f\t%.1f\t%d\t%d\t%d\t", max / ((float) MILLION), java_insert_rate,
        fastutil_insert_rate,
        banana_insert_rate,
        java_used, fastutil_used, banana_used));

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


  public static int testBanana(int keys[]) {
    int num = keys.length;

    IHashMap h = new HashMap(num, RECORD_SIZE, 0, 1);
    for (int i = 0; i < keys.length; i++) {
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
    MemoryMXBean mx = ManagementFactory.getMemoryMXBean();
    MemoryUsage usage = mx.getHeapMemoryUsage();
    banana_used = usage.getUsed() / (1024 * 1024);
    // System.out.println("Memory used by Banana HashMap: " +
    // Util.formatSize(banana_used));

    return h.size();
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

  public static int testJava(int keys[]) {

    java.util.HashMap<Long, JavaObject> h = new java.util.HashMap<Long, JavaObject>(keys.length, 1.0f);
    for (int i = 0; i < keys.length; i++) {
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
    MemoryMXBean mx = ManagementFactory.getMemoryMXBean();
    MemoryUsage usage = mx.getHeapMemoryUsage();
    java_used = usage.getUsed() / (1024 * 1024);
    // System.out.println("Memory used by java.util.HashMap<Long, JavaObject> : "
    // + Util.formatSize(java_used));
    return h.size();
  }

  public static int testFastUtil(int keys[]) {

    Long2ObjectMap<JavaObject> h = new Long2ObjectOpenHashMap<JavaObject>(keys.length,
        1.0f);
    for (int i = 0; i < keys.length; i++) {
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
    MemoryMXBean mx = ManagementFactory.getMemoryMXBean();
    MemoryUsage usage = mx.getHeapMemoryUsage();
    fastutil_used = usage.getUsed() / (1024 * 1024);
    // System.out.println("Memory used by fastutil Long2ObjectOpenHashMap : " +
    // Util.formatSize(fastutil_used));
    return h.size();
  }
}
