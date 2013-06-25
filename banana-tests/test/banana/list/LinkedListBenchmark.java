package banana.list;

import java.util.LinkedList;

import banana.list.DoubleLinkedList;

public class LinkedListBenchmark {

  public static void main(String[] args) {

    // @formatter:off
    int test[][] = {
        { 100, 1000000 },
        { 1000, 100000 },
        { 10000, 10000 },
        { 100000, 1000 },
        { 1000000, 100 },
        { 10000000, 10 },
        { 100000000, 1 },
    };
    // @formatter:on

    // long bananaLinkedListResults[] = new long[test.length];
    // long bananaDoubleLinkedList[] = new long[test.length];

    long javaResults[] = new long[test.length];

    long bananaLinkedList[] = new long[test.length];

    for (int i = 0; i < test.length; i++) {
      int listSize = test[i][0];
      int numIterations = test[i][1];
      javaResults[i] = javaLinkedList(listSize, numIterations);
      System.gc();
    }

    for (int i = 0; i < test.length; i++) {
      int listSize = test[i][0];
      int numIterations = test[i][1];
      bananaLinkedList[i] = bananaLinkedList(listSize, 1, numIterations);
      System.gc();
    }

    // for (int i = 0; i < test.length; i++) {
    // int listSize = test[i][0];
    // int numIterations = test[i][1];
    // bananaDoubleLinkedList[i] = benchmarkBananaDoubleLinkedList(listSize, 1,
    // numIterations);
    // System.gc();
    // }

    for (int i = 0; i < test.length; i++) {
      long java = javaResults[i];
      long banana = bananaLinkedList[i];
      System.out.println(String.format("List with %d items: Banana %d ms / Java %d ms : %%%f",
          test[i][0], banana, java, 100 * (banana / (float) java)));
    }

  }

  public static long bananaDoubleLinkedList(int listSize, int recordSize, int numIterations) {
    long t = System.currentTimeMillis();
    DoubleLinkedList list = new banana.list.DoubleLinkedList(listSize, recordSize, 0);

    for (int nn = Short.MAX_VALUE; nn < Short.MAX_VALUE + numIterations; nn++) {
      // fill up list with items
      for (int i = 0; i < listSize; i++) {
        int n = list.appendTail(recordSize);
        list.setInt(n, 0, i);
      }

      // empty list in order
      while (list.getHead() != -1) {
        list.removeHead();
      }
    }

    long e = System.currentTimeMillis() - t;
    System.out.println("crunching Banana DoubleLinkedList of " + listSize + " items "
        + numIterations + " times took " + e + " ms");
    return e;
  }

  public static long bananaLinkedList(int listSize, int recordSize, int numIterations) {
    long t = System.currentTimeMillis();
    banana.list.LinkedList list = new banana.list.LinkedList(listSize,
        recordSize, 0.0);

    for (int nn = Short.MAX_VALUE; nn < Short.MAX_VALUE + numIterations; nn++) {
      // fill up list with items
      for (int i = 0; i < listSize; i++) {
        int n = list.appendTail(recordSize);
        list.setInt(n, 0, i);
      }

      // empty list in order
      while (list.getHead() != -1) {
        list.removeHead();
      }
    }

    long e = System.currentTimeMillis() - t;
    System.out.println("crunching LinkedList of " + listSize + " items " + numIterations
        + " times took " + e + " ms");
    return e;
  }

  public static long javaLinkedList(int listSize, int numIterations) {
    long t = System.currentTimeMillis();
    LinkedList<Integer> list = new LinkedList<Integer>();

    for (int nn = Short.MAX_VALUE; nn < Short.MAX_VALUE + numIterations; nn++) {
      // fill up list with items
      for (int i = 0; i < listSize; i++) {
        list.add(i);
      }

      // empty list in order
      while (list.size() > 0) {
        list.removeFirst();
      }
    }

    long e = System.currentTimeMillis() - t;
    System.out.println("crunching java list of " + listSize + " items " + numIterations
        + " times took " + e + " ms");
    return e;
  }

}
