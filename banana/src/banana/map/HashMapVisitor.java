package banana.map;

public interface HashMapVisitor {

  public void begin(IHashMap map);

  public void visit(IHashMap map, long key, int record_id, long num, long total);

  public void end(IHashMap map);
}