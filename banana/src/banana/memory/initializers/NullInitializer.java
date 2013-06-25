package banana.memory.initializers;

import banana.memory.IPrimitiveAccess;
import banana.memory.MemInitializer;

public class NullInitializer implements MemInitializer {

  public NullInitializer() {
  }

  @Override
  public void initialize(IPrimitiveAccess allocator, int pointer, int blockSize) {
    // noop
  }
}
