package net.yadan.banana.memory.malloc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.OutOfMemoryException;
import net.yadan.banana.memory.initializers.MemSetInitializer;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public abstract class AbstractCharsTest {

  protected IMemAllocator m;
  protected int m_allocationSize;

  public AbstractCharsTest(int numBlocks, int blockSize, int allocationSize) {
    m_allocationSize = allocationSize;
    init(numBlocks, blockSize);
  }

  public abstract void init(int numBlocks, int blockSize);

  @Parameters
  public static Collection<Object[]> data() {
    //@formatter:off
    Object[][] data = new Object[][] {
        { 100, 5, 5}, // normal non indexed block
        { 100, 5, 10},// single index block
        { 100, 5, 20},// single index block, full utilization
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

  @Override
  public String toString() {
    return m.toString();
  }


  @Test
  public void testSetCharsFullBlocks() {
    int p = m.malloc(m_allocationSize);
    int numChars = m_allocationSize * 2;
    char buf[] = new char[numChars];
    for (int i = 0; i < numChars; i++) {
      buf[i] = (char) ('a' + i);
    }

    m.setChars(p, 0, buf, 0, numChars);

    for (int i = 0; i < numChars; i++) {
      char e = (char) ('a' + i);
      int ii = i / 2;
      if (i % 2 == 0) {
        char c = (char) m.getUpperShort(p, ii);
        assertEquals(e, c);
      } else {
        char c = (char) m.getLowerShort(p, ii);
        assertEquals(e, c);
      }
    }

    m.free(p);
  }

  @Test
  public void testGetCharsFullBlocks() {
    int p = m.malloc(m_allocationSize);
    int numChars = m_allocationSize * 2;
    char buf[] = new char[numChars];
    for (int i = 0; i < numChars; i++) {
      buf[i] = (char) ('a' + i);
    }
    m.setChars(p, 0, buf, 0, numChars);

    char out[] = new char[numChars];
    m.getChars(p, 0, out, 0, numChars);
    assertArrayEquals(buf, out);
    m.free(p);
  }

  @Test
  public void testSetCharsFullBlocks_even() {
    int p = m.malloc(m_allocationSize);
    // uninitialized memory will be initialized to -1
    m.setInitializer(new MemSetInitializer(-1));
    
    int numChars = m_allocationSize * 2 - 1;
    char buf[] = new char[numChars];
    for (int i = 0; i < numChars; i++) {
      buf[i] = (char) ('a' + i);
    }

    m.setChars(p, 0, buf, 0, numChars);

    for (int i = 0; i < numChars; i++) {
      char e = buf[i];
      int ii = i / 2;
      if (i % 2 == 0) {
        char c = (char) m.getUpperShort(p, ii);
        assertEquals(e, c);
      } else {
        char c = (char) m.getLowerShort(p, ii);
        assertEquals(e, c);
      }
    }
    assertEquals(-1, m.getLowerShort(p, m_allocationSize - 1));
    m.free(p);
  }

  @Test
  public void testGetCharsFullBlocks_even() {
    int p = m.malloc(m_allocationSize);
    int numChars = m_allocationSize * 2;
    char buf[] = new char[numChars];
    for (int i = 0; i < numChars; i++) {
      buf[i] = (char) ('a' + i);
    }
    m.setChars(p, 0, buf, 0, numChars);

    char out[] = new char[numChars];
    m.getChars(p, 0, out, 0, numChars);
    assertArrayEquals(buf, out);
    m.free(p);
  }

  @Test
  public void testSetCharsPartialFirstBlock() {
    int p = m.malloc(m_allocationSize);
    int numChars = m_allocationSize * 2 - 2;
    char buf[] = new char[numChars];
    for (int i = 0; i < numChars; i++) {
      buf[i] = (char) ('a' + i);
    }

    m.setInt(p, 0, -100);
    m.setChars(p, 1, buf, 0, buf.length);

    assertEquals(-100, m.getInt(p, 0));
    for (int i = 0; i < numChars; i++) {
      char e = buf[i];
      int ii = i / 2 + 1;
      if (i % 2 == 0) {
        char c = (char) m.getUpperShort(p, ii);
        assertEquals(e, c);
      } else {
        char c = (char) m.getLowerShort(p, ii);
        assertEquals(e, c);
      }
    }

    m.free(p);
  }

  @Test
  public void testGetCharsPartialFirstBlock() {
    int p = m.malloc(m_allocationSize);
    int numChars = m_allocationSize * 2 - 2;
    char buf[] = new char[numChars];
    for (int i = 0; i < numChars; i++) {
      buf[i] = (char) ('a' + i);
    }

    m.setInt(p, 0, -100);
    m.setChars(p, 1, buf, 0, buf.length);

    char out[] = new char[numChars];
    m.getChars(p, 1, out, 0, numChars);
    assertArrayEquals(buf, out);
    m.free(p);
  }

  @Test
  public void testSetCharsPartialFirstBlockDeep() {
    int p = m.malloc(m_allocationSize);
    int dst_offset = m.blockSize() + 1;
    if (dst_offset > m_allocationSize) {
      dst_offset = m_allocationSize / 2;
    }

    for (int i = 0; i < dst_offset; i++) {
      m.setInt(p, i, 100 + i);
    }

    int numChars = (m_allocationSize - dst_offset) * 2;
    char buf[] = new char[numChars];
    for (int i = 0; i < numChars; i++) {
      buf[i] = (char) ('a' + i);
    }

    m.setChars(p, dst_offset, buf, 0, buf.length);

    // verify ints are correct using getInt
    for (int i = 0; i < dst_offset; i++) {
      assertEquals(100 + i, m.getInt(p, i));
    }

    for (int i = 0; i < numChars; i++) {
      char e = buf[i];
      int ii = i / 2 + dst_offset;
      if (i % 2 == 0) {
        char c = (char) m.getUpperShort(p, ii);
        assertEquals(e, c);
      } else {
        char c = (char) m.getLowerShort(p, ii);
        assertEquals(e, c);
      }
    }

    m.free(p);

  }

  @Test
  public void testGetCharsPartialFirstBlockDeep() {
    int src_offset = m.blockSize() + 1;
    if (src_offset > m_allocationSize) {
      src_offset = m_allocationSize / 2;
    }

    int p = m.malloc(m_allocationSize);
    int numChars = (m_allocationSize - src_offset) * 2;
    char buf[] = new char[numChars];
    for (int i = 0; i < numChars; i++) {
      buf[i] = (char) ('a' + i);
    }

    for (int i = 0; i < src_offset; i++) {
      m.setInt(p, i, 100 + i);
    }

    m.setChars(p, src_offset, buf, 0, buf.length);

    char buf2[] = new char[numChars];
    m.getChars(p, src_offset, buf2, 0, buf2.length);
    assertArrayEquals(buf, buf2);
    m.free(p);
  }

  @Test
  public void testSetCharsPartialLastBlock() {
    int p = m.malloc(m_allocationSize);
    int numChars = (m_allocationSize - 1) * 2;
    char buf[] = new char[numChars];
    for (int i = 0; i < numChars; i++) {
      buf[i] = (char) ('a' + i);
    }

    m.setInt(p, m_allocationSize - 1, -100);
    m.setChars(p, 0, buf, 0, buf.length);

    assertEquals(-100, m.getInt(p, m_allocationSize - 1));
    for (int i = 0; i < numChars; i++) {
      char e = buf[i];
      int ii = i / 2;
      if (i % 2 == 0) {
        char c = (char) m.getUpperShort(p, ii);
        assertEquals(e, c);
      } else {
        char c = (char) m.getLowerShort(p, ii);
        assertEquals(e, c);
      }
    }


    m.free(p);
  }

  @Test
  public void testGetCharsPartialLastBlock() {
    int p = m.malloc(m_allocationSize);
    int numChars = (m_allocationSize - 1) * 2;
    char buf[] = new char[numChars];
    for (int i = 0; i < numChars; i++) {
      buf[i] = (char) ('a' + i);
    }

    m.setInt(p, m_allocationSize - 1, -100);
    m.setChars(p, 0, buf, 0, buf.length);

    char buf2[] = new char[numChars];
    m.getChars(p, 0, buf2, 0, numChars);
    assertArrayEquals(buf, buf2);
    m.free(p);
  }

  @Test
  public void testSetGetCharsInLoop() {
    try {
      int maxBlocks = m.maxBlocks();
      int blockSize = m.blockSize();
      char buf[] = new char[(maxBlocks * 2) * blockSize];

      for (int i = 0; i < buf.length; i++) {
        buf[i] = (char) ('a' + i);
      }
      for (int numBlocks = 20; numBlocks < maxBlocks; numBlocks++) {

        try {
          int size = numBlocks * m.blockSize();
          int p = m.malloc(size);
          int numChars = size * 2;
          m.setChars(p, 0, buf, 0, numChars);

          char out[] = new char[numChars];
          m.getChars(p, 0, out, 0, numChars);

          for (int j = 0; j < size; j++) {
            assertEquals("Char at index " + j, buf[j], out[j]);
          }
          m.free(p);
          assertEquals(0, m.usedBlocks());
        } catch (AssertionError e) {
          System.out.println("Error, numBlocks = " + numBlocks);
          throw e;
        }
      }
    } catch (OutOfMemoryException e) {
      // expected to happen in this test.
    }
  }
}
