package net.yadan.banana.list;

/**
 * Java LinkedList vs Banana LinkedList 
 */
public class LinkedListBenchmark {
  public static void main(String[] args) {
    banana();
    java();
  }

  public static void banana() {
    long t = System.currentTimeMillis();

    // initial list size 16m records, block size 32 (storage is int[], so we
    // need 32 ints to hold 16 longs)
    net.yadan.banana.list.ILinkedList list = new DoubleLinkedList(16 * 1024 * 1024, 16 * 2, 0);

    // initial fill in
    for (int i = 0; i < 16L * 1024 * 1024; i++) {
      list.appendTail(32); // similar to java list.add() which appends to the
                           // end of the list
    }

    // the main stuff
    for (int i = 0; i < 16L * 1024 * 1024; i++) {
      list.removeHead(); // similar to java list removeFirst()
      list.appendTail(32); // similar to java list.add() which appends to the
                           // end of the list
    }

    System.out.println("Banana : " + (System.currentTimeMillis() - t) + " ms elapsed");
    float GB = 1024 * 1024 * 1024;
    long total = Runtime.getRuntime().totalMemory();
    long free = Runtime.getRuntime().freeMemory();
    System.out
        .printf(
            "Banana : total: %5.1f GB,  free: %5.1f GB, used = %5.1f GB, Banana reports that it's actually using %5.1f GB\n",
            total / GB, free / GB, (total - free) / GB, list.computeMemoryUsage() / GB);
  }

  public static void java() {

    long t = System.currentTimeMillis();

    java.util.LinkedList<long[]> list = new java.util.LinkedList<long[]>();

    // initial fill in
    for (int i = 0; i < 16L * 1024 * 1024; i++) {
      list.add(new long[16]);
    }

    // the main stuff
    for (int i = 0; i < 16L * 1024 * 1024; i++) {
      list.removeFirst();
      list.add(new long[16]);
    }

    System.out.println("Java : " + (System.currentTimeMillis() - t) + " ms elapsed");
    float GB = 1024 * 1024 * 1024;
    long total = Runtime.getRuntime().totalMemory();
    long free = Runtime.getRuntime().freeMemory();
    System.out.printf("Java : total: %5.1f GB,  free: %5.1f GB, used = %5.1f GB\n", total / GB, free / GB,
        (total - free) / GB);
  }
}
