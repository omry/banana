package net.yadan.banana.memory.block;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

//@formatter:off
@RunWith(Suite.class)
@SuiteClasses({
  BlockAllocatorTest.class,
  BigBlockAllocatorTest.class,
})
//@formatter:on
public class AllTests {
}
