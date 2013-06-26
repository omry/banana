package net.yadan.banana.memory;


public interface MemInitializer {
  public void initialize(IPrimitiveAccess allocator, int pointer, int blockSize);
}
