package net.yadan.banana.memory.malloc;

import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.initializers.MemSetInitializer;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;


@RunWith(value = Parameterized.class)
public abstract class AbstractMemSetTest {

  protected IMemAllocator m;
  protected int m_allocationSize;

  public AbstractMemSetTest(int numBlocks, int blockSize, int allocationSize) {
    m_allocationSize = allocationSize;
    init(numBlocks, blockSize);
  }

  public abstract void init(int numBlocks, int blockSize);

  @Parameters
  public static Collection<Object[]> data() {
    //@formatter:off
    Object[][] data = new Object[][] {
        { 100, 5, 5},
        { 100, 5, 10},
        { 100, 5, 20},
        { 200, 3, 7},
        { 200, 3, 14},
        { 200, 3, 60},
        { 50, 3, 9},
    };
    return Arrays.asList(data);
  }
  //@formatter:on

  @After
  public void postTest() {
    if (m != null) {
      assertEquals("Test leaks memory", 0, m.usedBlocks());
    }
    m = null;
  }

  @Test
  public void testMemSetFull() {
    int p = m.malloc(m_allocationSize);
    m.memSet(p, 0, m_allocationSize, 99);
    int buf[] = new int[m_allocationSize];
    m.getInts(p, 0, buf, 0, buf.length);
    for (int x : buf) {
      assertEquals(99, x);
    }
    m.free(p);
  }

  @Test
  public void testMemSet_secondHalf() {
    m.setInitializer(new MemSetInitializer(-1));
    int p = m.malloc(m_allocationSize);
    int middle = m_allocationSize / 2;
    m.memSet(p, middle, m_allocationSize - middle, 99);
    int half1[] = new int[middle];
    int half2[] = new int[m_allocationSize - middle];
    m.getInts(p, middle, half2, 0, half2.length);
    for (int x : half2) {
      assertEquals(99, x);
    }

    m.getInts(p, 0, half1, 0, half1.length);
    for (int x : half1) {
      assertEquals(-1, x);
    }

    m.free(p);
  }

  @Test
  public void testMemSet_firstHalf() {
    m.setInitializer(new MemSetInitializer(-1));
    int p = m.malloc(m_allocationSize);
    int middle = m_allocationSize / 2;
    m.memSet(p, 0, middle, 99);
    int half1[] = new int[middle];
    int half2[] = new int[m_allocationSize - middle];
    m.getInts(p, 0, half1, 0, half1.length);
    for (int x : half1) {
      assertEquals(99, x);
    }

    m.getInts(p, middle, half2, 0, half2.length);
    for (int x : half2) {
      assertEquals(-1, x);
    }

    m.free(p);
  }
}
