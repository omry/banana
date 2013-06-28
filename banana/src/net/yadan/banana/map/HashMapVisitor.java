/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.map;

public interface HashMapVisitor {

  public void begin(IHashMap map);

  public void visit(IHashMap map, long key, int record_id, long num, long total);

  public void end(IHashMap map);
}
