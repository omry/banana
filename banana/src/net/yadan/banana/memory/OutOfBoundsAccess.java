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
