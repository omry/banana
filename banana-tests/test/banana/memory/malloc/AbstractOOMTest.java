package banana.memory.malloc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import banana.memory.IMemAllocator;
import banana.memory.OutOfMemoryException;

@RunWith(value = Parameterized.class)
public abstract class AbstractOOMTest {

  protected IMemAllocator m;
  protected int m_allocationSize;
  protected boolean m_shouldOOM;

  public AbstractOOMTest(int numBlocks, int blockSize, int allocationSize,
      boolean shouldOOM) {
    m_allocationSize = allocationSize;
    m_shouldOOM = shouldOOM;
    init(numBlocks, blockSize);
  }

  abstract protected void init(int numBlocks, int blockSize);

  @Parameters
  public static Collection<Object[]> data() {
    //@formatter:off
    Object[][] data = new Object[][] {
        { 1, 5, 5, false},
        { 1, 5, 10, true},
        { 10, 5, 50, true},
        { 20, 5, 101, true},
        { 20, 5, 110, true},
        { 100, 5, 444, true},
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
  public void testOOM() {
    try {
      int p = m.malloc(m_allocationSize);
      m.free(p);
      assertFalse("Did not throw OutOfMemoryException", m_shouldOOM);
    } catch (OutOfMemoryException e) {
      assertFalse("Threw OutOfMemoryException", !m_shouldOOM);
    }
  }
}
