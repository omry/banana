/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory;


public interface MemInitializer {
  public void initialize(IPrimitiveAccess allocator, int pointer, int blockSize);
}
