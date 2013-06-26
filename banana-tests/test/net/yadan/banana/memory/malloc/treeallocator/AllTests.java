package net.yadan.banana.memory.malloc.treeallocator;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

//@formatter:off
@RunWith(Suite.class)
@SuiteClasses({
  TreeMemAllocatorTest.class,
  TreeOOMTest.class,
  TreeComputeMemoryUsageTest.class,
  TreeReallocTest.class,
  VarTests.class,
  TreeMemSetTest.class,
})
//@formatter:on
public class AllTests {
}