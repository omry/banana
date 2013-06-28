/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory;

public class OutOfBoundsAccess extends RuntimeException {

  public OutOfBoundsAccess() {
    super();
  }

  public OutOfBoundsAccess(String message, Throwable cause) {
    super(message, cause);
  }

  public OutOfBoundsAccess(String message) {
    super(message);
  }

  public OutOfBoundsAccess(Throwable cause) {
    super(cause);
  }

}
