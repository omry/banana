package banana;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

//@formatter:off
@RunWith(Suite.class)
@SuiteClasses({
  banana.memory.AllTests.class,
  banana.list.AllTests.class,
  banana.map.AllTests.class,
  banana.stack.AllTests.class,
  LRUTest.class,
  TextIndexTest.class,
})
//@formatter:on
public class AllTests {

}
