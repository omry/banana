package net.yadan.banana;

import com.google.caliper.Benchmark;
import com.google.caliper.runner.CaliperMain;

public class CaliperBenchmark extends Benchmark {
  public static void main(String[] args) {
    CaliperMain.main(CaliperBenchmark.class, args);
  }

  int a[];
  int b[];

  @Override
  protected void setUp() throws Exception {
    int len = 10000;
    a = new int[len];
    b = new int[len];
    for (int i = 0; i < a.length; i++) {
      a[i] = i + 1;
      b[b.length - (i + 1)] = i + 1;
    }
  }

  public int timeDiv(int reps) {
    int s = 0;
    for (int i = 0; i < reps; i++) {
      for (int j = 0; j < a.length; j++) {
        s ^= a[j] / b[j];
      }
    }
    return s;
  }
}
