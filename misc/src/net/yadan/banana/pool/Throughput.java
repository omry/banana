package net.yadan.banana.pool;

import java.util.concurrent.TimeoutException;

import net.yadan.banana.pool.ObjectsPool.Factory;
import net.yadan.banana.pool.ObjectsPool.PoolExhaustedPolicy;
import net.yadan.banana.pool.ObjectsPool.PooledObject;
import net.yadan.utils.Util;

public class Throughput {

  private static final class BenchThread extends Thread {

    private ObjectsPool m_pool;
    private long m_ops;
    private volatile boolean m_running = true;
    private volatile boolean exited = false;
    private int m_acquiresPerThread;
    PooledObject m_userland[];

    public BenchThread(ObjectsPool pool, int acquiresPerThread) {
      m_acquiresPerThread = acquiresPerThread;
      m_userland = new PooledObject[acquiresPerThread];
      m_pool = pool;
    }

    @Override
    public void run() {
      exited = false;
      try {
        while (m_running) {
          for (int i = 0; i < m_acquiresPerThread; i++) {
            m_userland[i] = m_pool.acquire();
            m_ops++;
          }
          for (int i = 0; i < m_acquiresPerThread; i++) {
            m_pool.release(m_userland[i]);
            m_userland[i] = null;
            m_ops++;
          }
        }
      } finally {
        exited = true;
      }
    }

    public void stopThread() {
      m_running = false;
    }

    public void await() throws InterruptedException {
      while (!exited) {
        Thread.sleep(1);
      }
    }
  }

  // test
  static class Foo {
    int a, b, c;
  }

  public static void main(String[] args) throws TimeoutException, InterruptedException {

    // a nice big number
    int num = 10000000;
    final ObjectsPool pool = new ObjectsPool(num, false, PoolExhaustedPolicy.BLOCK, new Factory() {
      @Override
      public Object create() {
        return new Foo();
      }
    });

    int numThreads = 1;
    long testLengthMs = 5000;

    final int acquiresPerThread = num / (numThreads + 1);
    BenchThread threads[] = new BenchThread[numThreads];

    for (int k = 0; k < numThreads; k++) {
      threads[k] = new BenchThread(pool, acquiresPerThread);
    }

    long start = System.currentTimeMillis();
    for (int k = 0; k < numThreads; k++) {
      threads[k].start();
    }

    while (System.currentTimeMillis() - start < testLengthMs) {
      Thread.sleep(10);
    }

    for (int k = 0; k < numThreads; k++) {
      threads[k].stopThread();
    }

    for (int k = 0; k < numThreads; k++) {
      threads[k].await();
    }

    double elapsed = System.currentTimeMillis() - start;

    long ops = 0;
    for (int k = 0; k < numThreads; k++) {
      ops += threads[k].m_ops;
      System.out.println("Thread " + k + "  : " + threads[k].m_ops);
    }

    System.out.println("Number of threads : " + numThreads);
    System.out.println("Total ops " + Util.formatNum(ops) + " over " + elapsed + " ms");
    System.out.println(Util.formatNum((ops / (elapsed / 1000f))) + " ops/sec");
  }
}
