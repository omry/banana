/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.map;

import net.yadan.banana.DebugLevel;
import net.yadan.banana.ICollection;
import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.IPrimitiveAccess;

public interface IVarKeyHashMap extends ICollection, IPrimitiveAccess {

  /**
   * @return true if empty
   */
  @Override
  public boolean isEmpty();

  public int createRecord(IBuffer key, int size);

  public int createRecord(IBuffer key, IBuffer value);

  /**
   * Reallocated the memory size
   * @param key
   * @param newSize
   * @return
   */
  public int reallocRecord(IBuffer key, int newSize);

  public boolean containsKey(IBuffer key);

  public int findRecord(IBuffer key);

  public boolean remove(IBuffer key);

  @Override
  public void clear();

  public int getCapacity();

  /**
   * @return number of records used in this hash-map
   */
  @Override
  public int size();

  public double getLoadFactor();

  /**
   * @param d growth factor. 0 to disable growth and d > 1 to support growth by
   *          this factor.
   */
  public void setGrowthFactor(double d);

  @Override
  public long computeMemoryUsage();

  /**
   * Visits each record in the hashtable, and enables the caller to run code for
   * each record
   *
   * @param visitor
   */
  public void visitRecords(VarKeyHashMapVisitor visitor);

  public IMemAllocator valueMemory();

  public IMemAllocator keysMemory();

  @Override
  public void setDebug(DebugLevel debug);
}
