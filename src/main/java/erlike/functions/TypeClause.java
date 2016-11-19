package erlike.functions;

import static java.util.Objects.requireNonNull;

/**
 * A clause that matches instances of {@code T}.
 *
 * @param <T> The type this clause matches.
 */
class TypeClause<T> implements Clause {
  /**
   * An instance of the type to be matched.
   */
  protected final Class<T> type;

  /**
   * The body which accepts the matched object.
   */
  protected final Lambda.One<T> body;

  /**
   * Create a new clause.
   */
  TypeClause(Class<T> type, Lambda.One<T> body) {
    this.type = requireNonNull(type);
    this.body = requireNonNull(body);
  }

  /**
   * Matches an object if it is an instance of type
   */
  public boolean matches(Object arg) {
    return type.isInstance(arg);
  }

  /**
   * Evaluate the clause for the given object.
   */
  @Override
  public void accept(Object arg) throws Exception {
    // I am tempted to catch ClassCastExceptions so I can give better error messages
    // but body.accept may throw one as well.
    body.accept(type.cast(arg));
  }
}
