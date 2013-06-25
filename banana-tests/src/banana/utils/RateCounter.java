package banana.utils;



/**
 * Created on 23/02/2004
 *
 * @author Omry Yadan
 */
public class RateCounter {
  private int m_averageOverNumSeconds;

  int m_seconds[];

  int m_currentSecond;

  float m_ticksPerSecond;

  // the actual number of seconds used in the array.
  private int m_actualArraySize;

  // the index of the current second in the seconds array.
  private int m_pointer;

  private long m_numTicks;

  public RateCounter(int numSeconds) {
    init(numSeconds);
  }

  /**
   * @param numSeconds
   */
  private synchronized void init(int numSeconds) {
    m_averageOverNumSeconds = numSeconds;
    m_seconds = new int[m_averageOverNumSeconds];
    m_currentSecond = -1;
    m_ticksPerSecond = -1;
    m_actualArraySize = 0;
    m_pointer = 0;
  }

  public void tick() {
    tick(1);
  }

  public void tick(int ticks) {
    tick(ticks, System.currentTimeMillis());
  }

  /**
   * @param ticks number of ticks
   * @param timestamp logical time this even happened. this can be used to rate count historical timestamped data
   */
  public synchronized void tick(int ticks, long time) {
    m_numTicks += ticks;
    int second = (int) (time / 1000);
    if (second != m_currentSecond && m_currentSecond != -1) // second changed
                                                            // and not first
    {
      m_actualArraySize = Math.min(m_actualArraySize + 1, m_seconds.length);
      m_pointer = (m_pointer + 1) % m_seconds.length;
      m_seconds[m_pointer] = ticks;

      int c = 0;
      for (int i = 0; i < m_actualArraySize; i++) {
        if (i != m_pointer) {
          c += m_seconds[i];
        }
      }

      if (m_actualArraySize > 2) {
        m_ticksPerSecond = c / (float) (m_actualArraySize - 1);
      }

      // System.out.print("counter = ");
      // for (int i = 0 ; i < m_actualArraySize ; i++)
      // {
      // System.out.print(m_seconds[i] + ",");
      // }
      // System.out.println("tps : " + getTicksPerSecond());
    } else // same second
    {
      m_seconds[m_pointer] += ticks;
    }
    m_currentSecond = second;
  }

  public synchronized float getTicksPerSecond() {
    return m_ticksPerSecond;
  }

  /**
   *
   */
  public void reset() {
    init(m_averageOverNumSeconds);
  }

  public long getNumTicks() {
    return m_numTicks;
  }

  private static class Ticker {
    private final RateCounter m_rc;
    private final int m_intervalMs;
    private final int m_numPulse;
    private boolean m_running = false;
    private final Thread m_thread;

    public Ticker(int intervalMs, int numPulse) {
      System.out.println("Interval " + intervalMs + ", pulse " + numPulse
          + ", avg = " + (numPulse / (intervalMs / 1000f)) + " pulses/sec");
      m_rc = new RateCounter(5);
      m_intervalMs = intervalMs;
      m_numPulse = numPulse;
      m_thread = new Thread() {
        @Override
        public void run() {
          while (m_running) {
            m_rc.tick(m_numPulse);
            try {
              sleep(m_intervalMs);
            } catch (InterruptedException e) {
            }
          }
        }
      };
      m_running = true;
      m_thread.start();
    }

    public void stop() {
      m_running = false;
      m_thread.interrupt();
      try {
        m_thread.join();
      } catch (InterruptedException e) {
      }
      System.out.println("Ticker exited");
    }
  }

  public static void main(String[] args) {
    System.out.println("Testing rate counter");
    // drive(new Ticker(1000, 1), 20);
    drive(new Ticker(10000, 10), 30);
  }

  private static void drive(Ticker t, int seconds) {
    while (seconds-- > 0) {
      try {
        Thread.sleep(1000);
        System.out.println("TPS : " + t.m_rc.getTicksPerSecond());
      } catch (InterruptedException e) {
      }
    }
    t.stop();
  }
}
