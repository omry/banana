/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana;

import net.yadan.banana.memory.IPrimitiveAccess;

public class DefaultFormatter implements Formatter {

  @Override
  public String format(IPrimitiveAccess parent, int pointer) {
    int max = parent.maximumCapacityFor(pointer);
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < max; i++) {
      int data = parent.getInt(pointer, i);
      sb.append(data);
      if (i != max - 1) {
        sb.append(",");
      }
    }
    sb.append("]");
    return sb.toString();
  }

}
