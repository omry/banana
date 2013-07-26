package net.yadan.banana.pool;

import java.util.concurrent.TimeoutException;

import net.yadan.banana.memory.OutOfMemoryException;
import net.yadan.banana.memory.block.BlockAllocator;

public class ObjectsPool {

  static enum PoolExhaustedPolicy {
    BLOCK, GROW
  }

  private PooledObject m_objects[];
  private BlockAllocator m_blocks;
  private Factory m_factory;
  private PoolExhaustedPolicy m_policy;

  public ObjectsPool(int poolSize, boolean lazy, PoolExhaustedPolicy policy, Factory factory) {
    m_policy = policy;
    m_factory = factory;
    m_blocks = new BlockAllocator(poolSize, 1, m_policy == PoolExhaustedPolicy.GROW ? 2 : 0);
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
      return acquire(0);
    } catch (TimeoutException e) {
      // can't happen
      assert false;
      return null;
    }
  }

  public PooledObject acquire(int timeout) throws TimeoutException {
    // exit from this loop is either by returning an object or by throwing a
    // timeout exception
    while (true) {
      try {
        int p;
        synchronized (m_blocks) {
          p = m_blocks.malloc();
        }
        if (m_objects[p] == null) {
          m_objects[p] = new PooledObject(m_factory.create());
        }
        m_objects[p].m_ptr = p;
        return m_objects[p];
      } catch (OutOfMemoryException e) {
        // can only happen if policy is BLOCK
        assert m_policy == PoolExhaustedPolicy.BLOCK;

        long now = System.currentTimeMillis();
        synchronized (m_blocks) {
          while (m_blocks.usedBlocks() == m_blocks.maxBlocks()) {
            System.out.println("full");
            try {
              m_blocks.wait(20);
            } catch (InterruptedException e1) {
            }

            if (timeout != 0 && System.currentTimeMillis() - now > timeout) {
              throw new TimeoutException();
            }
          }
        }
      }
    }
  }

  public void release(PooledObject obj) {
    synchronized (m_blocks) {
      m_blocks.free(obj.m_ptr);
      obj.m_ptr = -1; // just in case
      if (m_policy == PoolExhaustedPolicy.BLOCK) {
        m_blocks.notifyAll();
      }
    }
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
}
