package banana.memory.initializers;

import banana.memory.IPrimitiveAccess;
import banana.memory.MemInitializer;

public class MemSetInitializer implements MemInitializer {

  private int m_val;

  public MemSetInitializer(int val) {
    m_val = val;
  }

  @Override
  public void initialize(IPrimitiveAccess allocator, int pointer, int blockSize) {
    for (int i = 0; i < blockSize; i++) {
      allocator.setInt(pointer, i, m_val);
    }
  }

  @Override
  public String toString() {
    return MemSetInitializer.class.getSimpleName() + " ("+m_val+")";
  }
}
