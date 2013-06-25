package banana.memory;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

//@formatter:off
@RunWith(Suite.class)
@SuiteClasses({
  banana.memory.block.AllTests.class,
  banana.memory.malloc.AllTests.class,
  BufferTest.class,
})
//@formatter:on
public class AllTests {
}
