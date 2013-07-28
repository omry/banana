/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.list;

import net.yadan.banana.ICollection;
import net.yadan.banana.memory.IBuffer;
import net.yadan.banana.memory.IMemAllocator;
import net.yadan.banana.memory.IPrimitiveAccess;

public interface ILinkedList extends ICollection, IPrimitiveAccess {

  /**
   * Inserts a new head for the list
   *
   * @param size allocation size for new link
   * @return the id of the new link
   */
  public int insertHead(int size);

  /**
   * Inserts a new head for the list
   *
   * @param date a buffer containing the data for the new record, buffer content
   *          will be copied into a newly allocated allocated memory block
   * @return the id of the new link
   */
  public int insertHead(IBuffer data);

  /**
   * Removes the head of the list
   */
  public void removeHead();

  /**
   * Appends the link to the end of the list
   *
   * @param size allocation size for new link
   * @return the id of the new link
   */
  public int appendTail(int size);

  /**
   * Appends the link to the end of the list
   *
   * @param date a buffer containing the data for the new record, buffer content
   *          will be copied into a newly allocated allocated memory block
   * @return the id of the new link
   */
  public int appendTail(IBuffer data);

  /**
   * Inserts the specified link after the anchor
   *
   * @param size allocation size for new link
   * @param anchor the link after which to insert the new link
   * @return the id of the new link
   */
  public int insert(int size, int anchor);

  /**
   * Inserts the specified link after the anchor
   *
   * @param date a buffer containing the data for the new record, buffer content
   *          will be copied into a newly allocated allocated memory block
   * @param anchor the link after which to insert the new link
   * @return the id of the new link
   */
  public int insert(IBuffer data, int anchor);

  /**
   * Removes the specified link from the list. Note: depending on the
   * implementation, this may an O(n) operation
   *
   * @param link
   */
  public void remove(int link);

  /**
   * @return the number of records in the linked list
   */
  @Override
  public int size();

  /**
   * @return the head of the list
   */
  public int getHead();

  /**
   * @return the tail of the list
   */
  public int getTail();

  /**
   * Returns the link after the specified link
   *
   * @param link
   * @return
   */
  public int getNext(int link);

  /**
   * Returns the link before the specified link Note that this is only
   * implemented for Bi-Directional list, and will throw an
   * {@link UnsupportedOperationException} for LinkedList
   * 
   * @param link
   * @return
   */
  public int getPrev(int link);

  /**
   * @return the memory allocator used by this linked list
   */
  public IMemAllocator getAllocator();
}
