package erlike.functions;

/**
 * A clause that matches a type, but none of it's subtypes.
 *
 * @param <T> The exact type to match.
 */
class ExactTypeClause<T> extends TypeClause<T> {
  ExactTypeClause(Class<T> type, Lambda.One<T> body) {
    super(type, body);
  }

  /**
   * Matches any object that has *exactly* type T.
   */
  @Override
  public boolean matches(Object arg) {
    if (arg == null) {
      return false;
    }
    return type.equals(arg.getClass());
  }
}
