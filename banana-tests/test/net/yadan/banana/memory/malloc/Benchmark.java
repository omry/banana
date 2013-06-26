package net.yadan.banana.memory.malloc;

import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.initializers.MemSetInitializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class Benchmark {

  final int TOTAL_ALLOCATION_SIZE = 1 * 1024 * 1024;
  int BLOCK_SIZE = 16;
  final int NUM_BLOCKS = TOTAL_ALLOCATION_SIZE / BLOCK_SIZE;
  int VAR_SIZE_ALLOCATION = -1;

  IMemAllocator m_treeIntAllocator;
  ChainedAllocator m_chainedIntAllocator;

  @Rule
  public TestRule benchmarkRun = new BenchmarkRule();

  public Benchmark(int blockSize, int varSizeAllocationSize) {
    BLOCK_SIZE = blockSize;
    VAR_SIZE_ALLOCATION = varSizeAllocationSize;
  }

  @Parameters
  public static Collection<Object[]> data() {
    //@formatter:off
    Object[][] data = new Object[][] {
        {16, 256},
    };
    return Arrays.asList(data);
  }
  //@formatter:on

  @Before
  public void init() {
    m_treeIntAllocator = new TreeAllocator(NUM_BLOCKS, BLOCK_SIZE);
    m_treeIntAllocator.setDebug(true);
    m_treeIntAllocator.setInitializer(new MemSetInitializer(-1));
    m_chainedIntAllocator = new ChainedAllocator(NUM_BLOCKS, BLOCK_SIZE);
    m_chainedIntAllocator.setDebug(true);
    m_chainedIntAllocator.setInitializer(new MemSetInitializer(-1));
  }

  @After
  public void cleanup() {
    m_treeIntAllocator.clear();
    m_chainedIntAllocator.clear();
  }

  @Test
  public void java_testSingleBlockRead() {
    @SuppressWarnings("unused")
    long sum = 0;
    for (int i = 0; i < NUM_BLOCKS; i++) {
      int arr[] = new int[VAR_SIZE_ALLOCATION];
      for (int j = 0; j < VAR_SIZE_ALLOCATION; j++) {
        sum += arr[j];
      }
      arr = null; // "free"
    }
  }

  @Test
  public void java_testSingleBlockWrite() {
    for (int i = 0; i < NUM_BLOCKS; i++) {
      int arr[] = new int[VAR_SIZE_ALLOCATION];
      for (int j = 0; j < VAR_SIZE_ALLOCATION; j++) {
        arr[j] = j;
      }
      arr = null; // "free"
    }
  }

  @Test
  public void TreeIntAllocator_testSingleBlockRead() {
    intAllocIntRead(m_treeIntAllocator);
  }

  @Test
  public void ChainedIntAllocator_testSingleBlockRead() {
    intAllocIntRead(m_chainedIntAllocator);
  }

  @Test
  public void TreeIntAllocator_testSingleBlockWrite() {
    intAllocIntWrite(m_treeIntAllocator);
  }

  @Test
  public void ChainedIntAllocator_testSingleBlockWrite() {
    intAllocIntWrite(m_chainedIntAllocator);
  }

  @Test
  public void TreeIntAllocator_testSingleBlockArrayRead() {
    intAllocArrayRead(m_treeIntAllocator);
  }

  @Test
  public void ChainedIntAllocator_testSingleBlockArrayRead() {
    intAllocArrayRead(m_chainedIntAllocator);
  }

  @Test
  public void TreeIntAllocator_testSingleBlockArrayWrite() {
    intAllocArrayWrite(m_treeIntAllocator);
  }

  @Test
  public void ChainedIntAllocator_testSingleBlockArrayWrite() {
    intAllocArrayWrite(m_chainedIntAllocator);
  }

  public void intAllocIntRead(IMemAllocator allocator) {
    assertEquals(0, allocator.usedBlocks());
    @SuppressWarnings("unused")
    long sum = 0;
    for (int i = 0; i < allocator.maxBlocks(); i++) {
      int pointer = allocator.malloc(VAR_SIZE_ALLOCATION);
      for (int j = 0; j < allocator.blockSize(); j++) {
        sum += allocator.getInt(pointer, j);
      }
      allocator.free(pointer);
    }
  }

  public void intAllocIntWrite(IMemAllocator allocator) {
    assertEquals(0, allocator.usedBlocks());
    @SuppressWarnings("unused")
    long sum = 0;
    for (int i = 0; i < allocator.maxBlocks(); i++) {
      int pointer = allocator.malloc(VAR_SIZE_ALLOCATION);
      for (int j = 0; j < allocator.blockSize(); j++) {
        allocator.setInt(pointer, j, j);
      }
      allocator.free(pointer);
    }
  }

  public void intAllocArrayRead(IMemAllocator allocator) {
    assertEquals(0, allocator.usedBlocks());
    int block[] = new int[VAR_SIZE_ALLOCATION];
    for (int i = 0; i < allocator.maxBlocks(); i++) {
      int pointer = allocator.malloc(VAR_SIZE_ALLOCATION);
      for (int j = 0; j < allocator.blockSize(); j++) {
        allocator.getInts(pointer, 0, block, 0, VAR_SIZE_ALLOCATION);
      }
      allocator.free(pointer);
    }
  }

  public void intAllocArrayWrite(IMemAllocator allocator) {
    assertEquals(0, allocator.usedBlocks());
    int block[] = new int[VAR_SIZE_ALLOCATION];
    for (int i = 0; i < allocator.maxBlocks(); i++) {
      int pointer = allocator.malloc(VAR_SIZE_ALLOCATION);
      for (int j = 0; j < allocator.blockSize(); j++) {
        allocator.setInts(pointer, 0, block, 0, VAR_SIZE_ALLOCATION);
      }
      allocator.free(pointer);
    }
  }
}
