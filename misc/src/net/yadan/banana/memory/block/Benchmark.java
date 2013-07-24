/*
 * Copyright (C) 2013 omry <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory.block;

import java.util.Arrays;
import java.util.Collection;

import net.yadan.banana.memory.IBlockAllocator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class Benchmark {

  int m_pointers[];
  IBlockAllocator m_blocks;
  IBlockAllocator m_bigBlocks;

  public Benchmark(int maxBlocks, int blockSize) {
    m_blocks = new BlockAllocator(maxBlocks, blockSize);
    m_bigBlocks = new BigBlockAllocator(maxBlocks, blockSize);
    m_pointers = new int[maxBlocks];
    cleanup();
  }

  @Parameters
  public static Collection<Object[]> data() {
    //@formatter:off
    Object[][] data = new Object[][] {
        {10 * 100, 4},
        {10 * 100, 16},
    };
    return Arrays.asList(data);
  }
  //@formatter:on

  @Before
  public void init() {
  }

  @After
  public void cleanup() {
    m_blocks.clear();
    m_bigBlocks.clear();
    for (int i = 0; i < m_pointers.length; i++) {
      m_pointers[i] = -1;
    }
  }

  @Test
  public void benchMallocFree_BlocksAllocator() {
    benchMallocFree(m_blocks);
  }

  @Test
  public void benchMallocFree_BigBlocksAllocator() {
    benchMallocFree(m_bigBlocks);
  }

  @Test
  public void benchReadWrite_BlocksAllocator() {
    benchReadWrite(m_blocks);
  }

  @Test
  public void benchReadWrite_BigBlocksAllocator() {
    benchReadWrite(m_bigBlocks);
  }

  public void benchMallocFree(IBlockAllocator blocks) {
    int iterations = 10000;

    for (int k = 0; k < iterations; k++) {
      for (int i = 0; i < blocks.maxBlocks(); i++) {
        m_pointers[i] = blocks.malloc();
      }

      for (int i = 0; i < blocks.maxBlocks(); i++) {
        blocks.free(m_pointers[i]);
      }
    }
  }

  public void benchReadWrite(IBlockAllocator blocks) {
    int iterations = 10000;

    @SuppressWarnings("unused")
    long sum = 0;
    for (int k = 0; k < iterations; k++) {
      for (int i = 0; i < blocks.maxBlocks(); i++) {
        int p = blocks.malloc();
        for (int j = 0; j < blocks.blockSize(); j++) {
          blocks.setInt(p, j, i);
          sum += blocks.getInt(p, j);
        }
      }
      blocks.clear();
    }
  }
}
