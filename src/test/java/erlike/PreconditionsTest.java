package erlike;

import org.junit.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Created by alex on 11/20/16.
 */
public class PreconditionsTest {
  @Test(expected = NullPointerException.class)
  public void notNullExceptionTest() {
    Preconditions.checkNotNull(null);
  }
}
