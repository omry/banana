/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory;

/**
 * Methods common to all allocators.
 *
 * @author omry
 * created May 6, 2013
 */
public interface IAllocator extends IPrimitiveAccess {

  /**
   * Returns the specified pointer back into the allocator pool. The pointer
   * should not be used after it was freed.
   *
   * @param pointer
   */
  public void free(int pointer);

  /**
   * Sets the memory pointed to by a specific pointer
   * @param pointer pointer pointing to memory we want to set
   * @param srcPos source position
   * @param length number of ints to set
   * @param value value to set to
   */
  public void memSet(int pointer, int srcPos, int length, int value);

  /**
   * @return the underlying block size of this allocator
   */
  public int blockSize();

  /**
   * @return true if this allocator is in debug mode
   */
  boolean isDebug();

  /**
   * Turn the debug state on or off
   *
   * @param debug new debug state
   */
  void setDebug(boolean debug);

  /**
   * Sets the {@link MemInitializer} of this Allocator
   *
   * @param initializer
   */
  public void setInitializer(MemInitializer initializer);

  /**
   * @return the number of used blocks
   */
  public int usedBlocks();

  /**
   * @return the maximum block currently available in this allocator. Note:
   *         unless growth is disabled, the allocator will allocate additional
   *         blocks when it's required to allocate blocks beyond the max blocks
   *         count.
   */
  public int maxBlocks();

  /**
   * @return the current number of free blocks.
   */
  public int freeBlocks();

  /**
   * Resets this allocator. all previously allocated pointers should no longer
   * be used after this is called.
   */
  public void clear();

  /**
   * Returns an estimation of the number of bytes this allocator is using
   */
  public long computeMemoryUsage();

  /**
   * Sets the allocator growth factor.
   *
   * @param d new growth factor, 0 to disable growth (default)
   */
  public void setGrowthFactor(double d);

  /**
   * @return the current list growth factor
   */
  public double getGrowthFactor();

  /**
   * Initializes the pointer with the current initializer
   * @param pointer
   */
  void initialize(int pointer);
}
