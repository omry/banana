/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.experimental.set;

public interface ISetVisitor {

  /**
   * gets called once when we start iteration
   */
  public void begin(ISet set);

  /**
   * gets called for each element in the set
   */
  public void visit(ISet set, int ptr);

  /**
   * gets called once when we finish iteration
   */
  public void end(ISet set);
}
