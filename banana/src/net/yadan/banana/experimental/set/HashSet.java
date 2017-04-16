package net.yadan.banana.experimental.set;

import net.yadan.banana.DebugLevel;
import net.yadan.banana.DefaultFormatter;
import net.yadan.banana.Formatter;
import net.yadan.banana.memory.Buffer;
import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.malloc.TreeAllocator;

public class HashSet implements ISet {

  private static final double DEFAULT_GROWTH_FACTOR = 2.0;

  private static final int NEXT_OFFSET = 0;
  private static final int KEY_SIZE_OFFSET = 1;
  private static final int KEY_DATA_OFFSET = 2;

  IMemAllocator m_memory;
  private final double m_loadFactor;
  private double m_growthFactor;

  private int m_size;

  /**
   * The table is rehashed when its size exceeds this threshold. (The value of
   * this field is (int)(capacity * loadFactor).)
   */
  private int m_threshold;

  /**
   * Holds an array of pointers into m_memory
   */
  private int m_table[];

  private Formatter m_formatter;

  private DebugLevel m_debugLevel = DebugLevel.NONE;

  public HashSet(int maxBlocks, int blockSize, double growthFactor, double loadFactor) {
    this(new TreeAllocator(maxBlocks, blockSize, growthFactor), maxBlocks, loadFactor);
  }

  public HashSet(IMemAllocator memory, int initialCapacity, double loadFactor) {
    m_size = 0;
    m_loadFactor = loadFactor;
    m_growthFactor = DEFAULT_GROWTH_FACTOR;
    m_table = new int[initialCapacity];
    m_threshold = (int) Math.min(getCapacity() * getLoadFactor(), Integer.MAX_VALUE);

    for (int i = 0; i < m_table.length; i++) {
      m_table[i] = -1;
    }

    m_formatter = new DefaultFormatter();
    m_memory = memory;
  }

  @Override
  public DebugLevel getDebug() {
    return m_debugLevel;
  }

  @Override
  public void setFormatter(Formatter formatter) {
    m_formatter = formatter;
  }

  @Override
  public Formatter getFormatter() {
    return m_formatter;
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public int insert(IBuffer element) {
    if (size() >= m_threshold && m_growthFactor > 0) {
      increaseCapacity();
    }

    int listNum = hashCode(element, m_table.length);
    int pointer = m_table[listNum];

    // find if this key is already in the chain
    while (pointer != -1) {
      int keySize2 = m_memory.getInt(pointer, KEY_SIZE_OFFSET);

      // list already contain this element, nothing to do
      if (element.equals(m_memory, pointer, KEY_DATA_OFFSET, keySize2)) {
        break;
      }
      pointer = m_memory.getInt(pointer, NEXT_OFFSET);
    }

    if (pointer == -1) {
      int size = element.size();
      int ptr = m_memory.malloc(size + KEY_DATA_OFFSET);
      m_memory.setInt(ptr, KEY_SIZE_OFFSET, size);
      m_memory.setInts(ptr, KEY_DATA_OFFSET, element.array(), 0, size);
      m_table[listNum] = pointer;
      m_size++;
    }

    return pointer;
  }

  private void increaseCapacity() {
    // long t = Sysftem.currentTimeMillis();
    IBuffer buffer = new Buffer(10, 2);
    int capacity = getCapacity();
    long newCapacity = Math.max(capacity + 1, (long) (capacity * m_growthFactor));
    if (newCapacity > Integer.MAX_VALUE) {
      throw new IllegalStateException("Attempted to resize table to " + newCapacity
          + " which is greated than Integer.MAX_VALUE");
    }
    int intCap = (int) newCapacity;
    int newTable[] = new int[intCap];
    for (int i = 0; i < newTable.length; i++) {
      newTable[i] = -1;
    }

    for (int tableNum = 0; tableNum < m_table.length; tableNum++) {
      int n = m_table[tableNum];
      while (n != -1) {
        int keySize = m_memory.getInt(n, KEY_SIZE_OFFSET);
        buffer.ensureCapacity(keySize);
        m_memory.getInts(n, KEY_DATA_OFFSET, buffer.array(), 0, keySize);
        buffer.setUsed(keySize);
        int newTableNum = hashCode(buffer, intCap);
        buffer.reset();

        int next = m_memory.getInt(n, NEXT_OFFSET);
        m_memory.setInt(n, NEXT_OFFSET, newTable[newTableNum]);
        newTable[newTableNum] = n;
        n = next;
      }
    }

    m_table = newTable;
    m_threshold = (int) Math.min(getCapacity() * getLoadFactor(), Integer.MAX_VALUE);
    // System.out.println(String.format("Increased map capacity from %d to %d took %d ms",
    // capacity,
    // intCap, (System.currentTimeMillis() - t)));
  }

  @Override
  public boolean contains(IBuffer element) {
    return findElement(element) != -1;
  }

  @Override
  public int findElement(IBuffer element) {
    int listNum = hashCode(element, m_table.length);

    int n = m_table[listNum];
    while (n != -1) {
      int keySize = m_memory.getInt(n, KEY_SIZE_OFFSET);
      if (element.equals(m_memory, n, KEY_DATA_OFFSET, keySize)) {
        break;
      }
      n = m_memory.getInt(n, NEXT_OFFSET);
    }

    return n;
  }

  @Override
  public boolean remove(IBuffer element) {
    int listNum = hashCode(element, m_table.length);

    int n = m_table[listNum];
    int prev = -1;
    boolean first = true;
    while (n != -1) {
      int keySize = m_memory.getInt(n, KEY_SIZE_OFFSET);
      if (element.equals(m_memory, n, KEY_DATA_OFFSET, keySize)) {
        int next = m_memory.getInt(n, NEXT_OFFSET);
        if (first) {
          m_table[listNum] = next;
        } else {
          m_memory.setInt(prev, NEXT_OFFSET, next);
        }
        m_size--;
        m_memory.free(n);
        return true;
      }
      prev = n;
      first = false;
      n = m_memory.getInt(n, NEXT_OFFSET);
    }

    return false;

  }

  @Override
  public void clear() {
    m_size = 0;
    visitRecords(new DefaultSetVisitor() {
      @Override
      public void visit(ISet map, int keyPtr) {
        m_memory.free(keyPtr);
      }
    });
    for (int i = 0; i < m_table.length; i++) {
      m_table[i] = -1;
    }
  }

  @Override
  public int getCapacity() {
    return m_table.length;
  }

  @Override
  public int size() {
    return m_size;
  }

  @Override
  public double getLoadFactor() {
    return m_loadFactor;
  }

  @Override
  public void setGrowthFactor(double d) {
    m_growthFactor = d;
  }

  @Override
  public long computeMemoryUsage() {
    return 4 * m_table.length + m_memory.computeMemoryUsage();
  }

  @Override
  public void setDebug(DebugLevel level) {
    m_debugLevel = level;
  }

  @Override
  public IMemAllocator getAllocator() {
    return m_memory;
  }

  private int hashCode(IBuffer key, int listSize) {
    int h = key.hashCode();
    int r = h % listSize;
    return r < 0 ? r + listSize : r;
  }

  @Override
  public void visitRecords(ISetVisitor visitor) {
    visitor.begin(this);

    for (int i = 0; i < m_table.length; i++) {
      int n = m_table[i];
      while (n != -1) {
        int next = m_memory.getInt(n, NEXT_OFFSET);
        visitor.visit(this, n);
        n = next;
      }
    }
    visitor.end(this);
  }
}
