/*
 * Copyright (C) 2014 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.experimental.set;

import net.yadan.banana.DebugLevel;
import net.yadan.banana.ICollection;
import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IMemAllocator;

public interface ISet extends ICollection {

  @Override
  public boolean isEmpty();

  public int insert(IBuffer value);

  public boolean contains(IBuffer element);

  public int findElement(IBuffer element);

  public boolean remove(IBuffer element);

  @Override
  public void clear();

  public int getCapacity();

  @Override
  public int size();

  public double getLoadFactor();

  /**
   * @param d
   *          growth factor. 0 to disable growth and d > 1 to support growth by
   *          this factor.
   */
  public void setGrowthFactor(double d);

  @Override
  public long computeMemoryUsage();

  public void visitRecords(ISetVisitor visitor);

  @Override
  public void setDebug(DebugLevel level);

  public IMemAllocator getAllocator();
}
