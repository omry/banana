/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory.block;

import net.yadan.banana.memory.IBlockAllocator;

import com.google.caliper.AfterExperiment;
import com.google.caliper.BeforeExperiment;
import com.google.caliper.Benchmark;
import com.google.caliper.Param;

public class FloatAccess {

  @Param({ "100" })
  int m_numBlocks;

  @Param({ "BLOCK", "BIG_BLOCK" })
  Type m_type;

  IBlockAllocator m_blockAllocator;
  int m_pointer;

  BlockAllocator m_block;

  @BeforeExperiment
  protected void setUp() throws Exception {
    switch (m_type) {
    case BLOCK:
      m_blockAllocator = new BlockAllocator(m_numBlocks, 10);
      m_block = (BlockAllocator) m_blockAllocator;
    case BIG_BLOCK:
      m_blockAllocator = new BigBlockAllocator(m_numBlocks, 10);
    }

    m_pointer = m_blockAllocator.malloc();
  }

  @AfterExperiment
  public void cleanup() {
    m_blockAllocator.free(m_pointer);
  }

  @Benchmark
  public void timeSetFloat(long reps) {
    for (int i = 0; i < reps; i++) {
      m_blockAllocator.setFloat(m_pointer, 5, 20f);
    }
  }

  @Benchmark
  public void timeGetFloat(long reps) {
    for (int i = 0; i < reps; i++) {
      m_blockAllocator.getFloat(m_pointer, 5);
    }
  }
}
