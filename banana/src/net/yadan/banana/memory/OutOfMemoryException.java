/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.memory;

/**
 * Thrown by an allocator when an allocation is requested and there is no memory
 * to satisfy the request
 *
 * @author omry
 * @created 21/4/2013
 */
public class OutOfMemoryException extends RuntimeException {

  public OutOfMemoryException() {
    super();
  }

  public OutOfMemoryException(String message, Throwable cause) {
    super(message, cause);
  }

  public OutOfMemoryException(String message) {
    super(message);
  }

  public OutOfMemoryException(Throwable cause) {
    super(cause);
  }

}
