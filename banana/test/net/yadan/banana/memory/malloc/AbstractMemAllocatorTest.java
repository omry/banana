package net.yadan.banana.memory.malloc;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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
public abstract class AbstractMemAllocatorTest {

  protected IMemAllocator m;
  protected int m_allocationSize;

  public AbstractMemAllocatorTest(int numBlocks, int blockSize, int allocationSize) {
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

  @Test
  public void testUpperShort() {
    int p = m.malloc(m_allocationSize);
    int offset = 0;
    m.setInt(p, offset, 0);
    assertEquals(0, m.getLowerShort(p, offset));
    assertEquals(0, m.getUpperShort(p, offset));
    m.setUpperShort(p, offset, 99);
    assertEquals(0, m.getLowerShort(p, offset));
    assertEquals(99, m.getUpperShort(p, offset));

    offset = m_allocationSize / 2;
    m.setInt(p, offset, 0);
    assertEquals(0, m.getLowerShort(p, offset));
    assertEquals(0, m.getUpperShort(p, offset));
    m.setUpperShort(p, offset, 99);
    assertEquals(0, m.getLowerShort(p, offset));
    assertEquals(99, m.getUpperShort(p, offset));

    m.free(p);
  }

  @Test
  public void testLowerShort() {
    int p = m.malloc(m_allocationSize);
    int offset = 0;
    m.setInt(p, offset, 0);
    assertEquals(0, m.getLowerShort(p, offset));
    assertEquals(0, m.getUpperShort(p, offset));
    m.setLowerShort(p, offset, 99);
    assertEquals(99, m.getLowerShort(p, offset));
    assertEquals(0, m.getUpperShort(p, offset));

    offset = m_allocationSize / 2;
    m.setInt(p, offset, 0);
    assertEquals(0, m.getLowerShort(p, offset));
    assertEquals(0, m.getUpperShort(p, offset));
    m.setLowerShort(p, offset, 99);
    assertEquals(99, m.getLowerShort(p, offset));
    assertEquals(0, m.getUpperShort(p, offset));

    m.free(p);
  }

  @Test
  public void tesIntAccess() {
    int pointer = m.malloc(m_allocationSize);
    for (int i = 0; i < m_allocationSize; i++) {
      m.setInt(pointer, i, i);
    }

    for (int i = 0; i < m_allocationSize; i++) {
      assertEquals(i, m.getInt(pointer, i));
    }

    m.free(pointer);
  }

  @Test
  public void tesFloatAccess() {
    int pointer = m.malloc(m_allocationSize);
    for (int i = 0; i < m_allocationSize; i++) {
      m.setFloat(pointer, i, i * 2f);
    }

    for (int i = 0; i < m_allocationSize; i++) {
      assertEquals(i * 2f, m.getFloat(pointer, i), Float.MIN_VALUE);
    }

    m.free(pointer);
  }

  @Test
  public void tesDoubleAccess() {
    int pointer = m.malloc(m_allocationSize);
    for (int i = 0; i < m_allocationSize / 2; i++) {
      m.setDouble(pointer, i * 2, i * 3f);
    }

    for (int i = 0; i < m_allocationSize / 2; i++) {
      assertEquals(i * 3f, m.getDouble(pointer, i * 2), Float.MIN_VALUE);
    }

    m.free(pointer);
  }

  @Test
  public void testLong() {
    int p = m.malloc(10);
    m.setLong(p, 0, 0x00000000ffffffffL);
    assertEquals(0x00000000ffffffffL, m.getLong(p, 0));
    m.free(p);
  }

  @Test
  public void testLongOnBlockBoundary() {
    int p = m.malloc(10);
    m.setLong(p, 5, Long.MAX_VALUE);
    assertEquals(Long.MAX_VALUE, m.getLong(p, 5));
    m.free(p);
  }

  @Test
  public void testSetIntsFullBlocks() {
    int p = m.malloc(m_allocationSize);
    int buf[] = new int[m_allocationSize];
    for (int i = 0; i < m_allocationSize; i++) {
      buf[i] = i;
    }

    // set ints using setInts
    m.setInts(p, 0, buf, 0, m_allocationSize);

    // verify ints are correct using getInt
    for (int i = 0; i < m_allocationSize; i++) {
      assertEquals(i, m.getInt(p, i));
    }

    m.free(p);
  }

  @Test
  public void testGetIntsFullBlocks() {
    int p = m.malloc(m_allocationSize);
    int buf[] = new int[m_allocationSize];
    for (int i = 0; i < m_allocationSize; i++) {
      buf[i] = i;
    }
    for (int i = 0; i < buf.length; i++) {
      m.setInt(p, i, buf[i]);
    }

    int buf2[] = new int[m_allocationSize];
    m.getInts(p, 0, buf2, 0, m_allocationSize);
    assertArrayEquals(buf, buf2);
    m.free(p);
  }

  @Test
  public void testSetIntsPartialFirstBlock() {
    int p = m.malloc(m_allocationSize);
    int buf[] = new int[m_allocationSize - 1];
    for (int i = 0; i < buf.length; i++) {
      buf[i] = i;
    }

    m.setInt(p, 0, -100);
    m.setInts(p, 1, buf, 0, buf.length);

    // verify ints are correct using getInt
    assertEquals(-100, m.getInt(p, 0));
    for (int i = 0; i < buf.length; i++) {
      assertEquals(buf[i], m.getInt(p, i + 1));
    }

    m.free(p);
  }

  @Test
  public void testGetIntsPartialFirstBlock() {
    int p = m.malloc(m_allocationSize);
    int buf[] = new int[m_allocationSize - 1];
    for (int i = 0; i < m_allocationSize - 1; i++) {
      buf[i] = i;
    }

    m.setInt(p, 0, -100);
    for (int i = 0; i < buf.length; i++) {
      m.setInt(p, i + 1, buf[i]);
    }

    int buf2[] = new int[buf.length];
    m.getInts(p, 1, buf2, 0, buf.length);
    assertArrayEquals(buf, buf2);
    m.free(p);
  }

  @Test
  public void testSetIntsPartialFirstBlockDeep() {
    int p = m.malloc(m_allocationSize);
    int dst_offset = m.blockSize() + 1;
    if (dst_offset > m_allocationSize) {
      dst_offset = m_allocationSize / 2;
    }

    int buf[] = new int[m_allocationSize - dst_offset];
    for (int i = 0; i < buf.length; i++) {
      buf[i] = i;
    }

    for (int i = 0; i < dst_offset; i++) {
      m.setInt(p, i, 100 + i);
    }
    m.setInts(p, dst_offset, buf, 0, buf.length);

    // verify ints are correct using getInt
    for (int i = 0; i < dst_offset; i++) {
      assertEquals(100 + i, m.getInt(p, i));
    }

    for (int i = 0; i < buf.length; i++) {
      assertEquals(buf[i], m.getInt(p, i + dst_offset));
    }

    m.free(p);
  }

  @Test
  public void testGetIntsPartialFirstBlockDeep() {
    int src_offset = m.blockSize() + 1;
    if (src_offset > m_allocationSize) {
      src_offset = m_allocationSize / 2;
    }

    int p = m.malloc(m_allocationSize);
    int buf[] = new int[m_allocationSize - src_offset];
    for (int i = 0; i < m_allocationSize - src_offset; i++) {
      buf[i] = i;
    }

    for (int i = 0; i < src_offset; i++) {
      m.setInt(p, i, 100 + i);
    }
    for (int i = 0; i < buf.length; i++) {
      m.setInt(p, i + src_offset, buf[i]);
    }

    int buf2[] = new int[buf.length];
    m.getInts(p, src_offset, buf2, 0, buf2.length);
    assertArrayEquals(buf, buf2);
    m.free(p);
  }

  @Test
  public void testSetIntsPartialLastBlock() {
    int p = m.malloc(m_allocationSize);
    int buf[] = new int[m_allocationSize - 1];
    for (int i = 0; i < buf.length; i++) {
      buf[i] = i;
    }

    m.setInt(p, m_allocationSize - 1, -100);
    m.setInts(p, 0, buf, 0, buf.length);

    // verify ints are correct using getInt
    assertEquals(-100, m.getInt(p, m_allocationSize - 1));
    for (int i = 0; i < buf.length; i++) {
      assertEquals(buf[i], m.getInt(p, i));
    }

    m.free(p);
  }

  @Test
  public void testGetIntsPartialLastBlock() {
    int p = m.malloc(m_allocationSize);
    int buf[] = new int[m_allocationSize - 1];
    for (int i = 0; i < m_allocationSize - 1; i++) {
      buf[i] = i;
    }

    m.setInt(p, m_allocationSize - 1, -100);
    for (int i = 0; i < buf.length; i++) {
      m.setInt(p, i, buf[i]);
    }

    int buf2[] = new int[m_allocationSize - 1];
    m.getInts(p, 0, buf2, 0, m_allocationSize - 1);
    assertArrayEquals(buf, buf2);
    m.free(p);
  }

  @Test
  public void testMallocAndFree() {
    int pointer = m.malloc(m_allocationSize);
    m.free(pointer);
    assertEquals(0, m.usedBlocks());
  }

  @Test
  public void testSetGetIntsInLoop() {
    try {
      int maxBlocks = m.maxBlocks();
      int blockSize = m.blockSize();
      int buf[] = new int[maxBlocks * blockSize];

      for (int i = 0; i < maxBlocks * blockSize; i++) {
        buf[i] = i;
      }
      for (int numBlocks = 20; numBlocks < maxBlocks; numBlocks++) {

        try {
          int size = numBlocks * m.blockSize();
          int p = m.malloc(size);
          m.setInts(p, 0, buf, 0, size);

          int out[] = new int[size];
          m.getInts(p, 0, out, 0, size);

          for (int j = 0; j < size; j++) {
            assertEquals("Int at index " + j, buf[j], out[j]);
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

  @Test
  public void testBigAllocationForFirstBlock() {
    // first allocation pointer never be 0 or -1 because those are reserved for
    // signaling null/invalid pointer by data structures
    int p = m.malloc(m.blockSize() * 10);
    assertNotEquals(0, p);
    assertNotEquals(-1, p);
    m.free(p);
    p = m.malloc(m.blockSize());
    assertNotEquals(0, p);
    assertNotEquals(-1, p);
    m.free(p);
  }

  @Test
  public void testFreeInLoop() {
    for (int i = 0; i < m.maxBlocks(); i++) {
      try {
        int p = m.malloc(i * m.blockSize());
        m.free(p);
        assertEquals(0, m.usedBlocks());
      } catch (OutOfMemoryException e) {
        assertEquals(0, m.usedBlocks());
        break;
      }
    }
  }

  @Test
  public void testMemCopyFull() {
  }

  @Test
  public void testMemCopyPartialFirstBlock() {
  }

  @Test
  public void testInitialize() {
    m.setInitializer(new MemSetInitializer(-1));
    int size = 50;
    int p = m.malloc(size);
    try {
      int expected[] = new int[size]; // initialized to zeros
      m.memSet(p, 0, size, 0); // all zeros
      int out[] = new int[size];
      m.getInts(p, 0, out, 0, size);
      assertArrayEquals(expected, out);
      m.initialize(p);
      m.getInts(p, 0, out, 0, size);
      for (int i = 0; i < size; i++) {
        assertEquals(-1, out[i]);
      }
    } finally {
      m.free(p);
    }
  }

  @Override
  public String toString() {
    return m.toString();
  }
}
