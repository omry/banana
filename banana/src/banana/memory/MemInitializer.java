package banana.memory;


public interface MemInitializer {
  public void initialize(IPrimitiveAccess allocator, int pointer, int blockSize);
}
