package net.yadan.banana.memory.malloc.chainedallocator;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


//@formatter:off
@RunWith(Suite.class)
@SuiteClasses({
  ChainedMemAllocatorTest.class,
  ChainedOOMTest.class,
  ChainedComputeMemoryUsageTest.class,
  ChainedReallocTest.class,
  ChainedMemSetTest.class,
  ChainedCharsTest.class,
})
//@formatter:on
public class AllTests {
}