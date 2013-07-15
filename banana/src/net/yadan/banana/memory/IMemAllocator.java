/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory;

/**
 * Memory allocator interface
 *
 * @author omry
 * @created May 22, 2013
 */
public interface IMemAllocator extends IAllocator {

  /**
   * returns a pointer to a memory buffer large enough to hold size ints.
   */
  public int malloc(int size);

  /**
   * Changes the memory of the specified pointer to the new size and return a
   * new pointer
   *
   * @param pointer existing pointer, this pointer becomes invalid after this
   *          call and should no longere be used or freed
   * @param size new size, can be smaller or larger than original size
   * @return new pointer
   */
  public int realloc(int pointer, int size);

  /**
   * Computes the memory actual memory usage in bytes of an allocation that can
   * support the specified value
   */
  public int computeMemoryUsageFor(int size);

  public IBlockAllocator getBlocks();

  /**
   * will describe the pointer data structure, and optionally the data itself.
   * two pointers with identical allocation size should have the same pointer
   * structure.
   *
   * @param pointer the pointer
   * @return string debug representation of pointer data structure
   */
  public String pointerDebugString(int pointer);
}
