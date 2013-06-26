package net.yadan.banana.memory;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

//@formatter:off
@RunWith(Suite.class)
@SuiteClasses({
  net.yadan.banana.memory.block.AllTests.class,
  net.yadan.banana.memory.malloc.AllTests.class,
  BufferTest.class,
})
//@formatter:on
public class AllTests {
}
