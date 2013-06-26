package net.yadan.banana.stack;

import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.malloc.ChainedAllocator;


/**
 * MultiStack is basically an array of stacks, where each stack contains blocks of fixed size,
 * and the entire array of stacks is backed by a single IntAllocator.
 * @author omry
 * @created May 4, 2013
 */
public class MultiStack {

  private final static int NEXT_OFFSET = 0;
  private final static int DATA_OFFSET = 1;

  /**
   * Holds an array of pointers into m_memory
   */
  private int m_heads[];

  private IMemAllocator m_memory;
  private boolean m_debug;

  public MultiStack(int numStacks, int maxBlocks, int blockSize) {
    this(numStacks, maxBlocks, blockSize, 0);
  }

  public MultiStack(int numStacks, int maxBlocks, int blockSize, double growthFactor) {
    m_heads = new int[numStacks];
    m_memory = new ChainedAllocator(maxBlocks, blockSize + DATA_OFFSET, growthFactor);
    for (int i = 0; i < m_heads.length; i++) {
      m_heads[i] = -1;
    }
  }

  public void clear() {
    for (int i = 0; i < m_heads.length; i++) {
      clear(i);
    }
  }

  public void clear(int stackId) {
    int n = head(stackId);
    while(n != -1){
      n = pop(stackId);
    }
  }

  public int push(int stackId, int size) {
    int head = m_heads[stackId];
    int n = m_memory.malloc(size + DATA_OFFSET);
    m_memory.setInt(n, NEXT_OFFSET, head);
    m_heads[stackId] = n;
    return n;
  }


  public void push(int stackId, IBuffer data) {
    push(stackId, data.size() + DATA_OFFSET);
    m_memory.setInts(m_heads[stackId], DATA_OFFSET, data.array(), 0, data.size());

  }

  public int pop(int stackId) {
    ensure_not_empty(stackId);
    int head = m_heads[stackId];
    int next = m_memory.getInt(head, NEXT_OFFSET);
    m_memory.free(head);
    m_heads[stackId] = next;
    return next;
  }

  public void setInt(int stackId, int data, int offset_in_data) {
    ensure_not_empty(stackId);
    m_memory.setInt(m_heads[stackId], DATA_OFFSET + offset_in_data, data);
  }

  public void setLong(int stackId, long data, int offset_in_data) {
    ensure_not_empty(stackId);
    m_memory.setLong(m_heads[stackId], DATA_OFFSET + offset_in_data, data);
  }

  public void setInts(int stackId, int src_data[], int src_pos, int length,
      int dst_offset_in_record) {
    ensure_not_empty(stackId);
    m_memory.setInts(m_heads[stackId], DATA_OFFSET
        + dst_offset_in_record, src_data, src_pos, length);
  }

  public int getInt(int stackId, int offset_in_data) {
    ensure_not_empty(stackId);
    return m_memory.getInt(m_heads[stackId], DATA_OFFSET + offset_in_data);
  }

  public long getLong(int stackId, int offset_in_data) {
    ensure_not_empty(stackId);
    return m_memory.getLong(m_heads[stackId], DATA_OFFSET + offset_in_data);
  }

  public void getInts(int stackId, int src_offset_in_record, int dst_data[], int dst_pos,
      int length) {
    ensure_not_empty(stackId);
    m_memory.getInts(m_heads[stackId], DATA_OFFSET + src_offset_in_record, dst_data, dst_pos,
        length);
  }

  public boolean isEmpty(int stackId) {
    return m_heads[stackId] == -1;
  }

  public int availableBlocks() {
    return m_memory.freeBlocks();
  }

  public int usedBlocks() {
    return m_memory.usedBlocks();
  }

  public int maxBlock() {
    return m_memory.maxBlocks();
  }

  public int blockSize() {
    return m_memory.blockSize() - DATA_OFFSET;
  }

  private void ensure_not_empty(int stackId) {
    if (isEmpty(stackId)) {
      throw new IllegalStateException("Stack empty");
    }
  }

  public int head(int stackId) {
    return m_heads[stackId];
  }

  public int getNumStacks() {
    return m_heads.length;
  }

  public void ensureNumStacks(int numStacks) {
    if (getNumStacks() < numStacks) {
      int[] table = new int[numStacks];
      System.arraycopy(m_heads, 0, table, 0, m_heads.length);
      m_heads = table;
    }
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder("MultiStack with " + m_heads.length + " stacks");
    try {
      if (m_debug) {
        s.append("\n");
        for (int stackNum = 0; stackNum < m_heads.length; stackNum++) {
          s.append("Stack " + stackNum + " : ");
          if (isEmpty(stackNum)) {
            s.append("Empty");
          } else {
            int n = head(stackNum);
            while (n != -1) {
              s.append('(');
              s.append("#").append(n);
              s.append(",N:#" + m_memory.getInt(n, NEXT_OFFSET)).append(",");
              int dataSize = m_memory.blockSize() - DATA_OFFSET;
              for (int i = 1; i < dataSize; i++) {
                s.append(m_memory.getInt(n, i));
                if (i != m_memory.blockSize() - DATA_OFFSET - 1) {
                  s.append(',');
                }
              }
              s.append(')');

              n = m_memory.getInt(n, NEXT_OFFSET);
              if (n != -1) {
                s.append(",");
              }
            }
          }
          s.append("\n");
        }
      }
    } catch (RuntimeException e) {
      s.append(" :: Exception inToString() " + e.getClass().getName() + " : " + e.getMessage());
    }
    return s.toString();
  }

  public void setDebug(boolean debug) {
    m_debug = debug;
    m_memory.setDebug(debug);
  }
}
