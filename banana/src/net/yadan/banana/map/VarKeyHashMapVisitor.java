/*
 * Copyright (C) 2013 Omry Yadan <omry@yadan.net>
 * All rights reserved.
 *
 * See https://github.com/omry/banana/blob/master/BSD-LICENSE for licensing information
 */
package net.yadan.banana.map;

public interface VarKeyHashMapVisitor {

  public void begin(IVarKeyHashMap map);

  public void visit(IVarKeyHashMap map, int keyPtr, int valuePtr, long num, long total);

  public void end(IVarKeyHashMap map);
}
