package banana.memory.malloc;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

//@formatter:off
@RunWith(Suite.class)
@SuiteClasses({
  banana.memory.malloc.treeallocator.AllTests.class,
  banana.memory.malloc.chainedallocator.AllTests.class,
})
//@formatter:on
public class AllTests {
}

