package banana;

import static org.junit.Assert.assertEquals;
import banana.LRU;

public class LRUBenchmark {
  public static void main(String[] args) {

    int K = 1000;
    int M = 1000000;
    int test[][] = { { 50 * K, M }, { M, M }, { 3 * M, 20 * M }, { 20 * M, 100 * M },
        { 50 * M, 100 * M }, { 100 * M, 200 * M } };

    for (int k = 0; k < test.length; k++) {
      long t = System.currentTimeMillis();
      int cachesize = test[k][0];
      int inserts = test[k][1];
      LRU lru = new LRU(cachesize, false);
      for (long i = 0; i < inserts; i++) {
        lru.add(i);
      }

      long e = System.currentTimeMillis() - t;
      System.out.println(String.format(
          "LRU size = %d, Processed %d lru inserts in %d ms (%d inserts/sec)", cachesize, inserts,
          e, ((int) (inserts / (e / 1000.0)) * 100) / 100));
      assertEquals(cachesize, lru.size());
    }
  }
}
