/*
 * Copyright (C) ${year} Omry Yadan <${email}>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.map;

import net.yadan.banana.DebugLevel;
import net.yadan.banana.memory.Buffer;
import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.initializers.NullInitializer;
import net.yadan.banana.memory.malloc.MultiSizeAllocator;
import net.yadan.banana.memory.malloc.TreeAllocator;
import net.yadan.utils.StringUtil;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

public class Benchmark {

  private int m_num;

  public Benchmark(int num) {
    m_num = num;
  }

  public static Collection<Object[]> data() {
    //@formatter:off
    Object[][] data = new Object[][] {
        {2},
//        {1000},
//        {10000},
//        {100000},
//        {1000000},
//        {6500000},
//        {10000000},
//        {50000000},
//        {100000000},
    };
    return Arrays.asList(data);
  }
  //@formatter:on

  public void testBananaVarKeyHash() {
    NullInitializer nullInitializer = new NullInitializer();
    IMemAllocator values = new TreeAllocator(100, VarKeyHashMap.RESERVED_SIZE + 2, 1.2);
    IMemAllocator keys = new MultiSizeAllocator(100, new int[] { 2, 3, 5, 6 }, 1.2);
    values.setInitializer(nullInitializer);
    keys.setInitializer(nullInitializer);
    IVarKeyHashMap h = new VarKeyHashMap(values, keys, m_num, 1.0);
    h.setDebug(DebugLevel.DEBUG_STRUCTURE);

    IBuffer key = new Buffer(10);
    for (int i = 1; i < m_num; i++) {
      setAsString(key, i);
      int n = h.createRecord(key, 2);
      h.setLong(n, 0, i);
      key.reset();
    }

    for (int i = 0; i < m_num; i++) {
//      setAsString(key, i);
      key.appendChars((key + "_" + i).toCharArray());
      int n = h.findRecord(key);
      long v = h.getLong(n, 0);
      if (i != v)
        throw new RuntimeException("failed");
      key.reset();
    }


    System.gc();
    System.out.println(h.size());
    MemoryMXBean mx = ManagementFactory.getMemoryMXBean();
    MemoryUsage usage = mx.getHeapMemoryUsage();
    System.out.println("Memory used by banana varkey hashmap : " + usage.getUsed());
    System.out.println("Reported memory by banana varkey hashmap : " + h.computeMemoryUsage());
  }


  char chars[] = new char[33];

  private void setAsString(IBuffer key, int val) {
    int length = StringUtil.toChars(val, chars);
    key.appendChars(chars, 0, length);
  }

  public void testJava() {
    HashMap<String, Long> h = new HashMap<String, Long>(m_num, 1.0f);
    for (int i = 0; i < m_num; i++) {
      h.put(String.valueOf(i), new Long(i));
    }

    for (int i = 0; i < m_num; i++) {
      long v = h.get(String.valueOf(i));
      if (i != v)
        throw new RuntimeException("failed");
    }

    System.gc();
    System.out.println(h.size());
    MemoryMXBean mx = ManagementFactory.getMemoryMXBean();
    MemoryUsage usage = mx.getHeapMemoryUsage();
    System.out.println("Memory used by java hashmap : " + usage.getUsed());
  }

  public static void main(String[] args) throws Throwable {
    Collection<Object[]> data = data();
    for (Object o[] : data) {
      Integer size = (Integer) o[0];
      Benchmark b = new Benchmark(size);
      b.testBoth(size);
    }
  }

  private void testBoth(int size) {
    long t;
    t = System.currentTimeMillis();
    testJava();
    System.out.println(String.format("Java (%d items) : %d ms", size,
        (System.currentTimeMillis() - t)));
    System.gc();

    t = System.currentTimeMillis();
    testBananaVarKeyHash();
    System.out.println(String.format("banana varkey hash (%d items) : %d ms", size,
        (System.currentTimeMillis() - t)));
    System.gc();

  }
}
