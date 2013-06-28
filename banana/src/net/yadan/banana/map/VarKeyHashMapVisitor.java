package net.yadan.banana.map;

public interface VarKeyHashMapVisitor {

  public void begin(IVarKeyHashMap map);

  public void visit(IVarKeyHashMap map, int keyPtr, int valuePtr, long num, long total);

  public void end(IVarKeyHashMap map);
}
