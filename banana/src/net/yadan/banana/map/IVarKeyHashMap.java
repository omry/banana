package net.yadan.banana.map;

import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.IPrimitiveAccess;

public interface IVarKeyHashMap extends IPrimitiveAccess {

  /**
   * @return true if empty
   */
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

  public void clear();

  public int getCapacity();

  /**
   * @return number of records used in this hash-map
   */
  public int size();

  public double getLoadFactor();

  /**
   * @param d growth factor. 0 to disable growth and d > 1 to support growth by
   *          this factor.
   */
  public void setGrowthFactor(double d);

  /**
   * Returns an estimation of the number of bytes this HashMap is using
   */
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

  public void setDebug(DebugLevel debug);


}
