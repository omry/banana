/*
 * Copyright (C) ${year} Omry Yadan <${email}>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.map;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import net.yadan.utils.JVMSpawn;
import net.yadan.utils.Util;

public class LongToFixedSizeObjectBenchmark {

  public static int MILLION = 1000 * 1000;

  /**
   * Java object representing some fixed some data structure of size
   * 
   * <pre>
   *     name             : size in bytes
   *     -----------------:--------------
   *     long session_id  : 8
   *     byte ipv6[16]    : 16
   *     short port;      : 2
   *     short type;      : 2
   *     ----------------------
   *     total bytes      | 28
   * </pre>
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

  /**
   * Raw banana offsets (in ints) for the Java Object representation
   * 
   * <pre>
   *     name             : ints size : OFFSET
   *     -----------------------------------
   *     long session_id  : 2         : 0
   *     byte ipv6[16]    : 4         : 2
   *     short port;      : 0.5       : 6 (upper short)
   *     short type;      : 0.5       : 6 (lower short)
   *     ----------------------
   *     total ints       : 7
   * </pre>
   */

  public static int RECORD_SIZE = 7;
  public static int SESSION_ID_OFFSET = 0;
  public static int IPV6_OFFSET = 2;
  public static int PORT_OFFSET = 6; // upper short
  public static int TYPE_OFFSET = 6; // lower short

  public static int testBanana(String desc, int keys[], Stats stats) {
    long t;
    long e;
    int num = keys.length;
    int max = keys.length;
    t = System.currentTimeMillis();

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

    e = System.currentTimeMillis() - t;
    stats.insert_rate = max / (e / 1000f);
    System.out.println(String.format("%s : %s/inserts sec", desc, Util.formatNum(stats.insert_rate)));
    t = System.currentTimeMillis();

    for (int i = 0; i < keys.length; i++) {
      int key = keys[i];
      int r = h.findRecord(key);
      if (h.getLong(r, SESSION_ID_OFFSET) != key) {
        throw new RuntimeException("failed");
      }
    }

    e = System.currentTimeMillis() - t;
    stats.get_rate = max / (e / 1000f);
    System.out.println(String.format("%s : %s/gets sec", desc, Util.formatNum(stats.get_rate)));

    t = System.currentTimeMillis();
    h = new HashMap(num, RECORD_SIZE, 0, 1);
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

      r = h.findRecord(key);
      if (h.getLong(r, SESSION_ID_OFFSET) != key) {
        throw new RuntimeException("failed");
      }
    }

    e = System.currentTimeMillis() - t;
    stats.mixed_rate = max / (e / 1000f);
    System.out.println(String.format("%s : %s/inserts+gets (mixed) sec", desc, Util.formatNum(stats.mixed_rate)));
    
    System.gc();
    MemoryMXBean mx = ManagementFactory.getMemoryMXBean();
    MemoryUsage usage = mx.getHeapMemoryUsage();
    stats.mem_used = usage.getUsed();
    System.out.println(String.format("%s : %s used", desc, Util.formatSize(stats.mem_used)));


    return h.size();
  }

  public static int testJava(String desc, int keys[], Stats stats) {
    long t;
    long e;
    int num = keys.length;
    int max = keys.length;
    t = System.currentTimeMillis();

    java.util.HashMap<Long, JavaObject> h = new java.util.HashMap<Long, JavaObject>(num, 1.0f);
    for (int i = 0; i < num; i++) {
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

    e = System.currentTimeMillis() - t;
    stats.insert_rate = max / (e / 1000f);
    System.out.println(String.format("%s : %s/inserts sec", desc, Util.formatNum(stats.insert_rate)));
    t = System.currentTimeMillis();

    for (int i = 0; i < num; i++) {
      long key = keys[i];
      JavaObject r = h.get(key);
      if (r.session_id != key)
        throw new RuntimeException("failed");
    }

    e = System.currentTimeMillis() - t;
    stats.get_rate = max / (e / 1000f);
    System.out.println(String.format("%s : %s/gets sec", desc, Util.formatNum(stats.get_rate)));

    h = new java.util.HashMap<Long, JavaObject>(num, 1.0f);
    for (int i = 0; i < num; i++) {
      long key = keys[i];
      JavaObject o = new JavaObject(key);
      o.ipv6[0] = 1;
      o.ipv6[1] = 2;
      o.ipv6[2] = 3;
      o.ipv6[3] = 4;
      o.port = 9999;
      o.type = 0;
      h.put(key, o);

      JavaObject r = h.get(key);
      if (r.session_id != key)
        throw new RuntimeException("failed");
    }

    e = System.currentTimeMillis() - t;
    stats.mixed_rate = max / (e / 1000f);
    System.out.println(String.format("%s : %s/inserts+gets (mixed) sec", desc, Util.formatNum(stats.mixed_rate)));

    System.gc();
    MemoryMXBean mx = ManagementFactory.getMemoryMXBean();
    MemoryUsage usage = mx.getHeapMemoryUsage();
    stats.mem_used = usage.getUsed();
    System.out.println(String.format("%s : %s used", desc, Util.formatSize(stats.mem_used)));

    return h.size();
  }

  public static int testFastUtil(String desc, int keys[], Stats stats) {
    long t;
    long e;
    int num = keys.length;
    int max = keys.length;
    t = System.currentTimeMillis();

    Long2ObjectMap<JavaObject> h = new Long2ObjectOpenHashMap<JavaObject>(num, 1.0f);
    for (int i = 0; i < num; i++) {
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

    e = System.currentTimeMillis() - t;
    stats.insert_rate = max / (e / 1000f);
    System.out.println(String.format("%s : %s/inserts sec", desc, Util.formatNum(stats.insert_rate)));
    t = System.currentTimeMillis();

    for (int i = 0; i < num; i++) {
      long key = keys[i];
      JavaObject r = h.get(key);
      if (r.session_id != key)
        throw new RuntimeException("failed");
    }

    e = System.currentTimeMillis() - t;
    stats.get_rate = max / (e / 1000f);
    System.out.println(String.format("%s : %s/gets sec", desc, Util.formatNum(stats.get_rate)));

    h = new Long2ObjectOpenHashMap<JavaObject>(num, 1.0f);
    for (int i = 0; i < num; i++) {
      long key = keys[i];
      JavaObject o = new JavaObject(key);
      o.ipv6[0] = 1;
      o.ipv6[1] = 2;
      o.ipv6[2] = 3;
      o.ipv6[3] = 4;
      o.port = 9999;
      o.type = 0;
      h.put(key, o);

      JavaObject r = h.get(key);
      if (r.session_id != key)
        throw new RuntimeException("failed");
    }

    e = System.currentTimeMillis() - t;
    stats.mixed_rate = max / (e / 1000f);
    System.out.println(String.format("%s : %s/inserts+gets (mixed) sec", desc, Util.formatNum(stats.mixed_rate)));

    System.gc();
    MemoryMXBean mx = ManagementFactory.getMemoryMXBean();
    MemoryUsage usage = mx.getHeapMemoryUsage();
    stats.mem_used = usage.getUsed();
    System.out.println(String.format("%s : %s used", desc, Util.formatSize(stats.mem_used)));

    return h.size();
  }

  // / -------------------------
  // This section deals with launching a new JVM instance for each iteration.
  // Ideally this would be a part of a generic framework (or I would use an
  // existing generic framework like jmh or caliper)

  static class Stats {
    int max;
    float insert_rate;
    float get_rate;
    float mixed_rate;
    long mem_used;

    public Stats(int max) {
      this.max = max;
    }
  }

  static enum Type {
    Java, Banana, FastUtil
  }

  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      createFile("", Type.Java);
      createFile("", Type.Banana);
      createFile("", Type.FastUtil);
      // createFile("warmup_", Type.Java);
      // createFile("warmup_", Type.Banana);
      // createFile("warmup_", Type.FastUtil);

      int STEP = 5;
      for (int i = 12; i < 25; i++) {
        int m = i * STEP * MILLION;
        testType(Type.Java, m);
        testType(Type.FastUtil, m);
        testType(Type.Banana, m);
      }
    } else {

      int max = Integer.parseInt(args[1]);
      System.out.println(max / (float) MILLION + " million items");
      int keys[] = new int[max];
      for (int i = 0; i < max; i++) {
        keys[i] = i;
      }
      Util.shuffleArray(keys);

      Type type = Type.valueOf(args[0]);
      Stats stats = new Stats(max);
      runTypeTest("Warmup", keys, stats, type);
      // append("warmup_" + type.toString(), stats);

      // and now for real
      stats = new Stats(max);
      runTypeTest("Benchmark", keys, stats, type);
      append(type.toString(), stats);
    }
  }

  private static void runTypeTest(String desc, int[] keys, Stats stats, Type type) {
    String d = type + " : " + desc;
    switch (type) {
    case Java:
      testJava(d, keys, stats);
      break;
    case FastUtil:
      testFastUtil(d, keys, stats);
      break;
    case Banana:
      testBanana(d, keys, stats);
      break;
    default:
      throw new IllegalArgumentException("Unknown type " + type);
    }
  }

  private static void testType(Type t, int max) {
    int r = JVMSpawn.spawn("JVM For " + t + " (" + max + " items)", LongToFixedSizeObjectBenchmark.class.getName(),
        "-Xmx20g -XX:ParallelCMSThreads=1 -XX:ParallelGCThreads=1", new String[] { t.toString(), String.valueOf(max) });
    if (r != 0) {
      throw new RuntimeException("Error " + r);
    }
  }

  private static void createFile(String prefix, Type type) throws IOException {
    String header = String.format("%s,%s,%s,%s,%s\n", "Number if items", type + " insert rate", type + " get rate",
        type + " mixed get/sec rate", type + " memory used");

    Util.saveData(header.getBytes(), new File(prefix + type + ".csv"));
  }

  private static void append(String name, Stats stats) throws IOException {
    BufferedWriter out = null;
    try {
      int trys = 10;
      // in case someone is trying to open the file and it gets locked (ahem,
      // windows).
      while (true) {
        try {
          out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(name + ".csv", true)));
          break;
        } catch (IOException e) {
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e1) {
          }
          if (trys-- == 0)
            throw e;
        }
      }
      String line = String.format("%d,%.1f,%.1f,%.1f,%d\n", stats.max / MILLION, stats.insert_rate / MILLION,
          stats.get_rate / MILLION, stats.mixed_rate / MILLION, stats.mem_used / MILLION);
      out.append(line);
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }
}
