/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.list;

/**
 * Created by : omry
 * Date: 6/25/13
 */
public class LinkedListExample {

    public static void main(String[] args) {
        int maxRecords = 100;
        int blockSize = 5;
        double growthFactor = 2;

        // LinkedList is a single linked list
        // there is also DoubleLinkedList which offer bidirectional iteration and more efficient remove()
        linkedListExample(new LinkedList(maxRecords, blockSize, growthFactor));
    }

    private static void linkedListExample(ILinkedList list) {
        // insert a link with of int[5]
        int head = list.insertHead(5);
        list.setLong(head, 0, 99); // set a long on the first two ints
        list.setInt(head, 2, 100); // set an int on at the third slot

        // append a tail of int[5]
        int tail = list.appendTail(5);

        // set copy the array [1,2,3,4,5] into the list tail
        list.setInts(tail, 0, new int[]{1, 2, 3, 4, 5}, 0, 5);

        // insert a link of int[5] after head (and before tail)
        int middle = list.insert(5, head);
        // set some values there as well
        list.setInts(middle, 0, new int[]{11, 12, 13, 14, 15}, 0, 5);

        printList(list); // [0,99,100,0,0]->[11,12,13,14,15]->[1,2,3,4,5]


        // removes the first element from the list
        list.removeHead();
        printList(list); // [11,12,13,14,15]->[1,2,3,4,5]

        // remove last element
        list.remove(tail);
        printList(list);  // [11,12,13,14,15]
    }

    private static void printList(ILinkedList list) {
        // iterate list
        int link = list.getHead();
        while (link != -1) {
            System.out.print("[");
            for (int i = 0; i < 5; i++) {
                System.out.print(list.getInt(link, i));
                if (i + 1 < 5) {
                    System.out.print(",");
                }
            }

            link = list.getNext(link);

            System.out.print("]");
            if (link != -1) {
                System.out.print("->");
            }
        }
        System.out.println("");
    }
}
