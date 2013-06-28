package net.yadan.banana.memory.malloc;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

//@formatter:off
@RunWith(Suite.class)
@SuiteClasses({
  net.yadan.banana.memory.malloc.treeallocator.AllTests.class,
  net.yadan.banana.memory.malloc.chainedallocator.AllTests.class,
})
//@formatter:on
public class AllTests {
}

