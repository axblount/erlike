package erlike;

/**
 * Created by alex on 11/20/16.
 */
class Preconditions {
  private Preconditions() {
  }

  public static <T> T checkNotNull(T arg)
      throws NullPointerException {
    if (arg == null) {
      throw new NullPointerException();
    }
    return arg;
  }

  public static <T> T checkNotNull(T arg, Object msg)
      throws NullPointerException {
    if (arg == null) {
      throw new NullPointerException(String.valueOf(msg));
    }
    return arg;
  }

  public static <T> T checkNotNull(T arg, String form, Object... args)
      throws NullPointerException {
    if (arg == null) {
      throw new NullPointerException(String.format(form, args));
    }
    return arg;
  }
}
