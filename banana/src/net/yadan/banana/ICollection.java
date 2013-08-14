/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana;


/**
 * Functions implemented by all Banana collections
 * 
 * @author omry
 */
public interface ICollection {
  
  /**
   * @return true if empty
   */
  public boolean isEmpty();

  /**
   * Removes all items from this collection
   */
  public void clear();
  
  /**
   * @return number of items in this collection
   */
  public int size();
  
  
  /**
   * Returns an estimation of the number of bytes this collection is using
   */
  public long computeMemoryUsage();
  
  /**
   * Sets the debug level for the toString function
   * @param level
   */
  public void setDebug(DebugLevel level);
  
  /**
   * @return the collection debug level
   */
  public DebugLevel getDebug();

  /**
   * @return a String representation of this collection, based on the DebugLevel
   */
  @Override
  public String toString();

  /**
   * Sets the collection formatter.
   * 
   * @param formatter
   */
  public void setFormatter(Formatter formatter);

  /**
   * @return the collection formatter
   */
  public Formatter getFormatter();
}
