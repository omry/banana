/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory.initializers;

import net.yadan.banana.memory.IPrimitiveAccess;
import net.yadan.banana.memory.MemInitializer;

public class NullInitializer implements MemInitializer {

  public NullInitializer() {
  }

  @Override
  public void initialize(IPrimitiveAccess allocator, int pointer, int blockSize) {
    // noop
  }
}
