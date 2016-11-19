package erlike.functions;

import java.util.function.Predicate;

/**
 * Created by alex on 11/18/16.
 */
class PredicateClause<T> extends TypeClause<T> {
  private final Predicate<T> predicate;

  PredicateClause(Class<T> type, Predicate<T> predicate, Lambda.One<T> body) {
    super(type, body);
    this.predicate = predicate;
  }

  @Override
  public boolean matches(Object arg) {
    // If super.matches then we know the arg is the correct type.
    return super.matches(arg) && predicate.test(type.cast(arg));
  }
}
