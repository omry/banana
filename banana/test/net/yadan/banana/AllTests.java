package net.yadan.banana;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

//@formatter:off
@RunWith(Suite.class)
@SuiteClasses({
  net.yadan.banana.memory.AllTests.class,
  net.yadan.banana.list.AllTests.class,
  net.yadan.banana.map.AllTests.class,
  net.yadan.banana.stack.AllTests.class,
  net.yadan.banana.utils.AllTests.class,
})
//@formatter:on
public class AllTests {

}
