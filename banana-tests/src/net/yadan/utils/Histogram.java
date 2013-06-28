/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.utils;



/**
 * Histogram class This class has two modes,
 *  1. Static mode:
 *    in this mode, logical min and max are known. any value above max or
 *    below min goes into a (-inf,min) or (max,inf) bucket.
 *  2. dynamic mode:
 *    in this mode, min and max are discovered online and histogram
 *    resolution changes accordingly.
 *
 *  if you know the mind and max of the buckets ahead of
 *  time it's best to use the static mode, otherwise use the dynamic mode.
 *  it's possible to hit the dynamic mode at what you think will be the min and max
 *  and it will update to accommodate as new values outside of boundries are encountered.
 *
 * @author omry
 *
 */
public class Histogram {

  enum Type {
    STATIC, DYNAMIC
  }

  private long m_histogram[];
  private long m_min;
  private long m_max;
  private Type m_type;

  private long m_negInfBucket = 0;
  private long m_posInfBucket = 0;

  public static Histogram createStatic(int numBuckets, long minValue,
      long maxValue) {
    return new Histogram(numBuckets, minValue, maxValue, Type.STATIC);
  }

  public static Histogram createDynamic(int numBuckets) {
    return new Histogram(numBuckets, Long.MAX_VALUE, Long.MIN_VALUE,
        Type.DYNAMIC);
  }

  public static Histogram createDynamic(int numBuckets, long minValue,
      long maxValue) {
    return new Histogram(numBuckets, minValue, maxValue, Type.DYNAMIC);
  }

  private Histogram(int numBuckets, long minValue, long maxValue, Type type) {
    init(numBuckets, minValue, maxValue, type);
  }

  private void init(int numBuckets, long min, long max, Type type) {
    m_type = type;
    if (m_type == Type.DYNAMIC && numBuckets % 2 != 0) {
      // Dynamic histogram requires an even number of buckets
      numBuckets++;
    }
    m_histogram = new long[numBuckets];
    m_min = min;
    m_max = max;
  }

  public void addToHistogram(double value) {
    updateHistogram(value, 1);
  }

  public void addToHistogram(double value, int histChange) {
    updateHistogram(value, histChange);
  }

  public void removeFromHistogram(double value) {
    updateHistogram(value, -1);
  }

  public void removeFromHistogram(double value, int histChange) {
    updateHistogram(value, -histChange);
  }

  public void reset() {
    m_negInfBucket = 0;
    m_posInfBucket = 0;
    for (int i = 0; i < m_histogram.length; i++) {
      m_histogram[i] = 0;
    }
  }

  public int getSize() {
    return m_histogram.length;
  }

  public long getNegInfBucket() {
    return m_negInfBucket;
  }

  public long getPosInfBucket() {
    return m_posInfBucket;
  }

  public double get(int index) {
    if (index == -1)
      return m_negInfBucket;
    if (index == m_histogram.length)
      return m_posInfBucket;

    return m_histogram[index];
  }

  private void updateHistogram(double value, int histChange) {
    if (m_type == Type.DYNAMIC) {

      if (m_min == Long.MAX_VALUE) {
        m_min = (long) value;
      }
      if (m_max == Long.MIN_VALUE) {
        m_max = (long) value;
      }

      while (value > m_max) {
        long r = (m_max - m_min);
        if (r == 0)
          r = 1;
        m_max = m_max + r;

        for (int i = 0; i < m_histogram.length / 2; i++) {
          m_histogram[i] = m_histogram[i * 2] + m_histogram[i * 2 + 1];
        }

        for (int i = m_histogram.length / 2; i < m_histogram.length; i++) {
          m_histogram[i] = 0;
        }
      }

      while (value < m_min) {
        long r = (m_max - m_min);
        if (r == 0)
          r = 1;
        m_min = m_min - r;

        for (int i = m_histogram.length / 2, j = 0; i < m_histogram.length; i++, j += 2) {
          m_histogram[i] = m_histogram[j] + m_histogram[j + 1];
        }

        for (int i = 0; i < m_histogram.length / 2; i++) {
          m_histogram[i] = 0;
        }


      }

      double range = m_max - m_min;
      double normalized = (value - m_min) / range;
      int bucket = (int) Math.floor(normalized * m_histogram.length);
      if (bucket == m_histogram.length)
        bucket--;
      m_histogram[bucket] += histChange;

    } else if (m_type == Type.STATIC) {
      if (value >= m_min && value <= m_max) {
        double range = m_max - m_min;
        double normalized = (value - m_min) / range;
        int bucket = (int) Math.floor(normalized * m_histogram.length);
        if (bucket == m_histogram.length)
          bucket--;
        m_histogram[bucket] += histChange;
      } else {
        if (value > m_max) {
          // update (max,inf) bucket
          m_posInfBucket += histChange;
        } else if (value < m_min) {
          // update (-inf,min) bucket
          m_negInfBucket += histChange;
        }
      }
    }
  }
  @Override
  public String toString() {
    return toString(true);
  }

  public String toString(boolean prepandMinMax) {
    try {
      return histogramString(prepandMinMax);
    } catch (Exception e) {
      e.printStackTrace();
      return e.getMessage();
    }
  }

  public String histogramString(boolean prepandMinMax) {

    StringBuffer sb = new StringBuffer();
    long max = max(m_histogram);
    if (max == 0) max = 1;
    int width = (int) Math.floor(Math.log10(max) + 1) + 1;
    String format = "%" + width + "d";
    if (prepandMinMax)
      sb.append("min:" + m_min + ",max=" + m_max);
    sb.append("|");
    if (m_negInfBucket != 0){
      sb.append(m_negInfBucket).append("*|");
    }
    for (int i = 0; i < m_histogram.length; i++) {
      if (i != 0)
        sb.append("|");
      sb.append(String.format(format, m_histogram[i]));
    }

    if (m_posInfBucket != 0){
      sb.append("*").append(m_posInfBucket);
    }
    sb.append("|");
    return sb.toString();
  }

  private long max(long[] histogram) {
    long m = Long.MIN_VALUE;
    for(long d : histogram){
      m = Math.max(m, d);
    }
    return m;
  }

  public long getLowerBucketBoundry(int index) {
    if (index == -1)
      return Long.MIN_VALUE;
    else {
      double range = m_max - m_min;
      int bucket_width = (int) (range / m_histogram.length);
      return bucket_width * index;
    }
  }

  public long getUppetBucketBoundry(int index) {
    if (index == m_histogram.length) {
      return Long.MAX_VALUE;
    } else {
      double range = m_max - m_min;
      int bucket_width = (int) (range / m_histogram.length);
      return (bucket_width * (index + 1) - 1);
    }
  }

  public long[] getHistogram() {
    return m_histogram;
  }

  public long sum() {
    long t = 0;
    for(long l : m_histogram)
      t += l;
    return t;
  }

}
