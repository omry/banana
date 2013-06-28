/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory.initializers;

import net.yadan.banana.memory.IPrimitiveAccess;
import net.yadan.banana.memory.MemInitializer;

public class MemSetInitializer implements MemInitializer {

  private int m_val;

  public MemSetInitializer(int val) {
    m_val = val;
  }

  @Override
  public void initialize(IPrimitiveAccess allocator, int pointer, int blockSize) {
    for (int i = 0; i < blockSize; i++) {
      allocator.setInt(pointer, i, m_val);
    }
  }

  @Override
  public String toString() {
    return MemSetInitializer.class.getSimpleName() + " ("+m_val+")";
  }
}
