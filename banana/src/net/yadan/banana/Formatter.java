/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana;

import net.yadan.banana.memory.IPrimitiveAccess;


public interface Formatter {

  /**
   * Formats the record/item pointed to by pointer. this can be used to create
   * logical representations of collection items
   * 
   * @param parent
   * @param pointer
   * @return
   */
  public String format(IPrimitiveAccess parent, int pointer);
}
