package net.yadan.banana.list;

public class DoubleLinkedListTest extends LinkedListTest {

  @Override
  public ILinkedList createList(int maxBlocks, int blockSize, double growthFactor) {
    return new DoubleLinkedList(maxBlocks, blockSize, growthFactor);
  }

  @Override
  public ILinkedList createList(int maxBlocks, int sizes[], double growthFactor) {
    return new DoubleLinkedList(maxBlocks, sizes, growthFactor);
  }
}
