package banana.memory;

/**
 * Block allocator interface
 *
 * @author omry
 * @created May 22, 2013
 */
public interface IBlockAllocator extends IAllocator, IPrimitiveAccess {

  /**
   * @return a single block of fixed size (based on the allocator block size)
   */
  public int malloc();
}
