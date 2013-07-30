package net.yadan.banana.utils;

import net.yadan.banana.utils.LRU.Callback;
import net.yadan.banana.utils.LRU.DataType;

public class LRUExample {
  public static void main(String[] args) {
    int maxCapacity = 5;
    LRU lru = new LRU(maxCapacity, DataType.OBJECT);

    // adding 10 items to an LRU with size 5 with leave the last 5 items in
    for (int i = 0; i < 10; i++) {
      long key = i;
      lru.add(key, "Hello " + i);
    }

    System.out.println(lru.exists(4)); // false, 4 was evicted
    System.out.println(lru.get(5)); // Hello 5

    // callback for evicted items
    for (int i = 11; i < 15; i++) {
      long key = i;
      lru.add(key, "Hello " + i, new Callback() {
        @Override
        public void keyEvicted(long key, Object data) {
          System.out.println("Evicted " + data);
        }
      });
    }

    LRU primitiveLRU = new LRU(maxCapacity, DataType.LONG);
    // adding 10 items to an LRU with size 5 with leave the last 10 items i
    for (int i = 0; i < 10; i++) {
      long key = i;
      // no Boxing/Unboxing or memory allocation, pure primitive operation
      // function signature : void addLong(long id, long data)
      primitiveLRU.addLong(key, 100 + i);
    }
  }
}
