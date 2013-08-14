/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.list;

import net.yadan.banana.DebugLevel;

public class ListUtil {

  public static String listToString(ILinkedList list) {
    return listToString(list, list.getDebug());
  }

  public static String listToString(ILinkedList list, DebugLevel level) {
    try {
      StringBuilder sb = new StringBuilder("LinkedList (" + list.size() + " records)");
      switch (level) {
      case DEBUG_CONTENT: {
        sb.append("\n");
        int n = list.getHead();
        while (n != -1) {
          int next = list.getNext(n);
          String st;
          try {
            st = list.getFormatter().format(list, n);
          } catch (RuntimeException e) {
            st = "[" + e.getClass().getSimpleName() + " : " + e.getMessage() + "]";
          }
          sb.append(st);
          if (next != -1) {
            sb.append(" -> ");
          }
          n = next;
        }
      }
        break;
      case DEBUG_STRUCTURE: {
        sb.append("\n");
        int n = list.getHead();
        while (n != -1) {
          int next = list.getNext(n);
          sb.append("#").append(n).append(" : ");
          String st;
          try {
            st = list.getFormatter().format(list, n);
          } catch (RuntimeException e) {
            st = e.getClass().getSimpleName() + " : " + e.getMessage();
          }
          sb.append(st);
          if (next != -1) {
            sb.append(" -> ");
          }
          n = next;
        }
      }
        break;
      case NONE:
        break;
      }
      return sb.toString();
    } catch (RuntimeException e) {
      return "Exception in toString() : " + e.getClass().getSimpleName() + " : " + e.getMessage();
    }
  }

}
