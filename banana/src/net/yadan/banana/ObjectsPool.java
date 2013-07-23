package net.yadan.banana;

import net.yadan.banana.memory.OutOfMemoryException;
import net.yadan.banana.memory.block.BlockAllocator;

public class ObjectsPool {

  private PooledObject m_objects[];
  private BlockAllocator m_blocks;
  private Factory m_factory;

  public ObjectsPool(int poolSize, boolean lazy, Factory factory) {
    m_factory = factory;
    m_blocks = new BlockAllocator(poolSize, 1);
    // +1 because first block is reserved in the BlockAllocator and it will
    // never return index 0
    m_objects = new PooledObject[poolSize + 1];
    if (!lazy) {
      for (int i = 0; i < poolSize; i++) {
        m_objects[i] = new PooledObject(m_factory.create());
      }
    }
  }

  public PooledObject acquire() {
    try {
      int p = m_blocks.malloc();
      if (m_objects[p] == null) {
        m_objects[p] = new PooledObject(m_factory.create());
      }
      m_objects[p].m_ptr = p;
      return m_objects[p];
    } catch (OutOfMemoryException e) {
      // real world example would throw PoolExhaustedException or similar
      throw e;
    }
  }

  public void release(PooledObject obj) {
    m_blocks.free(obj.m_ptr);
    obj.m_ptr = -1; // just in case
  }

  public static class PooledObject {
    private int m_ptr = -1;
    Object m_object;

    public PooledObject(Object obj) {
      m_object = obj;
    }

    public Object get() {
      return m_object;
    }

  }

  public static interface Factory {
    public Object create();
  }

  // test
  static class Foo {
    int a, b, c;
  }
  public static void main(String[] args) {

    // a nice big number
    int num = 10000000;
    ObjectsPool pool = new ObjectsPool(num, false, new Factory() {

      @Override
      public Object create() {
        return new Foo();
      }
    });

    // temp place to keep objects user have ackquried
    PooledObject userland[] = new PooledObject[num];
    System.out.println("Initialization done");

    long t = System.currentTimeMillis();
    for (int i = 0; i < num; i++) {
      userland[i] = pool.acquire();
    }
    double e = (System.currentTimeMillis() - t);
    System.out.println(num / e + " acquires/ms");
    t = System.currentTimeMillis();
    for (int i = 0; i < num; i++) {
      pool.release(userland[i]);
      userland[i] = null;
    }
    e = (System.currentTimeMillis() - t);
    System.out.println(num / e + " frees/ms");
  }
}
