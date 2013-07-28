/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory;

/**
 * Block allocator interface
 *
 * @author omry
 * @created May 22, 2013
 */
public interface IBlockAllocator extends IAllocator {

  /**
   * @return a single block of fixed size (based on the allocator block size)
   */
  public int malloc();

  /**
   * Copies memory from point pointer to another within this allocator
   * 
   * @param srcPtr
   *          source pointer
   * @param srcPos
   *          source pointer position
   * @param dstPtr
   *          dest pointer
   * @param dstPos
   *          dest pointer position
   * @param length
   *          number of ints to copy
   */
  public void memCopy(int srcPtr, int srcPos, int dstPtr, int dstPos, int length);

}
