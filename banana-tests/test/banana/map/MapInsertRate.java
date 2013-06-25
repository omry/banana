package banana.map;

import gnu.trove.map.TLongLongMap;
import gnu.trove.map.hash.TLongLongHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

import banana.map.HashMap;
import banana.memory.IBlockAllocator;
import banana.memory.IMemAllocator;
import banana.memory.block.BigBlockAllocator;
import banana.memory.block.BlockAllocator;
import banana.memory.malloc.ChainedAllocator;
import banana.utils.Util;


public class MapInsertRate {

  public static void main(String[] args) throws InterruptedException {

    float lf = 0.96f;
    int max = 1 * 1000 * 1000 * 1000; // 1b
    max = 100 * 1000 * 1000;
    System.out.print("Initializing " + Util.formatNum(max) + " keys sequence...");
    int keys[] = new int[max];
    for (int i = 0; i < max; i++) {
      keys[i] = i;
    }
    System.out.print("Shuffling...");
    Util.shuffleArray(keys);
    System.out.println("Done");

    bananaRate(keys, lf);

    System.gc();
    Thread.sleep(5000);

    fastUtilRate(keys, lf);

    // System.gc();
    // Thread.sleep(5000);
    //
    // javaMapRate(keys, lf);

    System.gc();
    Thread.sleep(5000);

    troveMapRate(keys, lf);

  }

  public static int javaMapRate(int keys[], float loadFactor) {

    int max = keys.length;
    long start = System.currentTimeMillis();
    java.util.HashMap<Long, Long> map = new java.util.HashMap<>(max, loadFactor);
    System.out.println("java.util.HashMap init : " + (System.currentTimeMillis() - start));

    // SET
    int PRINT_BLOCK = 1000000;
    start = System.currentTimeMillis();
    long last_print = -1;
    for (int i = 0; i < max; i++) {
      if (i % PRINT_BLOCK == 0) {
        System.out.println("java.util.HashMap : Inserted " + i);
        if (last_print != -1) {
          long e = System.currentTimeMillis() - last_print;
          double rate = PRINT_BLOCK / (e / 1000f);
          System.out.println(String.format(
              "java.util.HashMap : Inserted %s items in %d ms, rate %s/sec ", Util.formatNum(i), e,
              Util.formatNum(rate)));
          last_print = System.currentTimeMillis();
        }
      }
      map.put((long) keys[i], (long) i);
    }

    long elapsed = System.currentTimeMillis() - start;
    System.out.printf("java.util.HashMap : Insert time %d, Avg rate %s / sec\n", elapsed,
        Util.formatNum((long) (max / (elapsed / 1000f))));

    // GET
    last_print = -1;
    start = System.currentTimeMillis();
    for (int i = 0; i < max; i++) {
      if (i % PRINT_BLOCK == 0) {
        if (last_print != -1) {
          long e = System.currentTimeMillis() - last_print;
          double rate = PRINT_BLOCK / (e / 1000f);
          System.out.println(String.format(
              "java.util.HashMap : Got %s items in %d ms, rate %s/sec ", Util.formatNum(i), e,
              Util.formatNum(rate)));
        }
        last_print = System.currentTimeMillis();
      }
      long n = map.get(keys[i]);
      if (i != n) {
        throw new RuntimeException("java.util.HashMap : Invalid value in map");
      }
    }

    elapsed = System.currentTimeMillis() - start;
    System.out.printf("java.util.HashMap : Get time %d, Avg rate %s / sec\n", elapsed,
        Util.formatNum((long) (max / (elapsed / 1000f))));

    System.gc();
    System.out.println("java.util.HashMap : used memory "
        + Util.formatSize((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())));
    return map.size();
  }

  public static int fastUtilRate(int keys[], float loadFactor) {
    int max = keys.length;
    long start = System.currentTimeMillis();
    Long2LongOpenHashMap map = new Long2LongOpenHashMap(max, loadFactor);
    System.out.println("FastUtil init : " + (System.currentTimeMillis() - start));

    // SET
    int PRINT_BLOCK = max / 10;
    start = System.currentTimeMillis();
    long last_print = -1;
    for (int i = 0; i < max; i++) {
      if (i % PRINT_BLOCK == 0) {
        if (last_print != -1) {
          long e = System.currentTimeMillis() - last_print;
          double rate = PRINT_BLOCK / (e / 1000f);
          System.out.println(String.format("FastUtil : Inserted %s items in %d ms, rate %s/sec ",
              Util.formatNum(i), e, Util.formatNum(rate)));
        }
        last_print = System.currentTimeMillis();
      }
      map.put(keys[i], i);
    }

    long elapsed = System.currentTimeMillis() - start;
    System.out.printf("FastUtil : Insert time %d, Avg rate %s / sec\n", elapsed,
        Util.formatNum((long) (max / (elapsed / 1000f))));

    // GET
    start = System.currentTimeMillis();
    last_print = -1;
    for (int i = 0; i < max; i++) {
      if (i % PRINT_BLOCK == 0) {
        if (last_print != -1) {
          long e = System.currentTimeMillis() - last_print;
          double rate = PRINT_BLOCK / (e / 1000f);
          System.out.println(String.format("FastUtil : Got %s items in %d ms, rate %s/sec ",
              Util.formatNum(i), e, Util.formatNum(rate)));
        }
        last_print = System.currentTimeMillis();
      }
      long n = map.get(keys[i]);
      if (i != n) {
        throw new RuntimeException("FastUtil : Invalid value in map");
      }
    }

    elapsed = System.currentTimeMillis() - start;
    System.out.printf("FastUtil : Get time %d, Avg rate %s / sec\n", elapsed,
        Util.formatNum((long) (max / (elapsed / 1000f))));

    System.gc();
    System.out.println("FastUtil : used memory "
        + Util.formatSize((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())));

    return map.size();
  }

  public static int troveMapRate(int keys[], float loadFactor) {

    int max = keys.length;
    long start = System.currentTimeMillis();
    TLongLongMap map = new TLongLongHashMap(max, loadFactor);
    System.out.println("TLongLongHashMap init : " + (System.currentTimeMillis() - start));

    // SET
    int PRINT_BLOCK = max / 10;
    start = System.currentTimeMillis();
    long last_print = -1;
    for (int i = 0; i < max; i++) {
      if (i % PRINT_BLOCK == 0) {
        if (last_print != -1) {
          long e = System.currentTimeMillis() - last_print;
          double rate = PRINT_BLOCK / (e / 1000f);
          System.out.println(String.format(
              "TLongLongHashMap : Inserted %s items in %d ms, rate %s/sec ", Util.formatNum(i), e,
              Util.formatNum(rate)));
        }
        last_print = System.currentTimeMillis();
      }
      map.put((long) keys[i], (long) i);
    }

    long elapsed = System.currentTimeMillis() - start;
    System.out.printf("TLongLongHashMap : Insert time %d, Avg rate %s / sec\n", elapsed,
        Util.formatNum((long) (max / (elapsed / 1000f))));

    // GET
    last_print = -1;
    start = System.currentTimeMillis();
    for (int i = 0; i < max; i++) {
      if (i % PRINT_BLOCK == 0) {
        if (last_print != -1) {
          long e = System.currentTimeMillis() - last_print;
          double rate = PRINT_BLOCK / (e / 1000f);
          System.out.println(String.format(
              "TLongLongHashMap : Got %s items in %d ms, rate %s/sec ", Util.formatNum(i), e,
              Util.formatNum(rate)));
        }
        last_print = System.currentTimeMillis();
      }
      long n = map.get(keys[i]);
      if (i != n) {
        throw new RuntimeException("TLongLongHashMap : Invalid value in map");
      }
    }

    elapsed = System.currentTimeMillis() - start;
    System.out.printf("TLongLongHashMap : Get time %d, Avg rate %s / sec\n", elapsed,
        Util.formatNum((long) (max / (elapsed / 1000f))));

    System.gc();
    System.out.println("TLongLongHashMap : used memory "
        + Util.formatSize((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())));

    return map.size();
  }

  public static int bananaRate(int keys[], float lf) {
    int max = keys.length;
    long start = System.currentTimeMillis();
    int mapCap = (int) Math.ceil(max * (1 / lf));
    IBlockAllocator blocks;
    if ((long) mapCap * (HashMap.RESERVED_SIZE + 2) > Integer.MAX_VALUE) {
      System.out.println("Using BigBlockAllocator");
      blocks = new BigBlockAllocator(mapCap, HashMap.RESERVED_SIZE + 2, 0);
    } else {
      System.out.println("Using BlockAllocator");
      blocks = new BlockAllocator(mapCap, HashMap.RESERVED_SIZE + 2, 0);
    }
    IMemAllocator memory = new ChainedAllocator(blocks);
    HashMap map = new HashMap(memory, mapCap, lf);
    System.out.println("Banana init : " + (System.currentTimeMillis() - start));

    // SET
    int PRINT_BLOCK = max / 10;
    start = System.currentTimeMillis();
    long last_print = -1;
    for (int i = 0; i < max; i++) {
      if (i % PRINT_BLOCK == 0) {
        if (last_print != -1) {
          long e = System.currentTimeMillis() - last_print;
          double rate = PRINT_BLOCK / (e / 1000f);
          System.out.println(String.format("Banana : Inserted %s items in %d ms, rate %s/sec ",
              Util.formatNum(i), e, Util.formatNum(rate)));
        }
        last_print = System.currentTimeMillis();
      }
      int n = map.createRecord(keys[i], 2);
      map.setLong(n, 0, i);
    }

    long elapsed = System.currentTimeMillis() - start;
    System.out.printf("Banana : Insert time %d, Avg rate %s / sec\n", elapsed,
        Util.formatNum((long) (max / (elapsed / 1000f))));

    // GET
    last_print = -1;
    start = System.currentTimeMillis();
    for (int i = 0; i < max; i++) {
      if (i % PRINT_BLOCK == 0) {
        if (last_print != -1) {
          long e = System.currentTimeMillis() - last_print;
          double rate = PRINT_BLOCK / (e / 1000f);
          System.out.println(String.format("Banana : Got %s items in %d ms, rate %s/sec ",
              Util.formatNum(i), e, Util.formatNum(rate)));
        }
        last_print = System.currentTimeMillis();
      }
      int n = map.findRecord(keys[i]);
      if (i != map.getLong(n, 0)) {
        throw new RuntimeException("Invalid value in map");
      }
    }

    elapsed = System.currentTimeMillis() - start;
    System.out.printf("Banana : Get time %d, Avg rate %s / sec\n", elapsed,
        Util.formatNum((long) (max / (elapsed / 1000f))));

    System.gc();
    System.out.println("Banana : used memory "
        + Util.formatSize((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())));
    System.out.println("Banana : reported memory usage " + Util.formatSize(map.computeMemoryUsage()));

    return map.size();

  }

}
