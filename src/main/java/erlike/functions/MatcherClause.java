package erlike.functions;

import org.hamcrest.Matcher;

/**
 * Created by alex on 11/18/16.
 */
class MatcherClause<T> extends TypeClause<T> {
  private final Matcher<T> matcher;

  MatcherClause(Class<T> type, Matcher<T> matcher, Lambda.One<T> body) {
    super(type, body);
    this.matcher = matcher;
  }

  @Override
  public boolean matches(Object arg) {
    return matcher.matches(arg);
  }

  @Override
  public void accept(Object arg) throws Exception {
    if (type.isInstance(arg) && matcher.matches(arg)) {
      body.accept(type.cast(arg));
    }
    throw new IllegalArgumentException();
  }
}
