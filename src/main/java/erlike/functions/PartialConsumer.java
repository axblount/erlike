/*
 * Copyright (C) 2015 Alex Blount <axblount@email.arizona.edu>
 *
 * This file is part of Erlike, see <https://github.com/axblount/erlike>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package erlike.functions;

import org.hamcrest.Matcher;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A {@link java.util.function.Consumer} that is only defined for certain types.
 */
public class PartialConsumer implements Lambda.One<Object> {

  /**
   * The clauses composing a PartialConsumer.
   */
  private List<TypeClause<?>> clauses;

  /**
   * Create a new PartialConsumer that matches no objects.
   */
  public PartialConsumer() {
    clauses = new LinkedList<>();
  }

  /**
   * Arguments for the consumer should first be checked with {@link #isDefinedAt(Object)}.
   *
   * @param arg The argument to be consumed.
   * @throws IllegalArgumentException Thrown when the partial consumer doesn't accept the given
   *                                  object.
   */
  @Override
  public void accept(Object arg) throws Exception {
    for (TypeClause<?> c : clauses) {
      if (c.matches(arg)) {
        c.accept(arg);
        return;
      }
    }

    throw new IllegalArgumentException("Partial function not defined at " + String.valueOf(arg));
  }

  /**
   * Determine if the PartialConsumer is defined at a given value, according to its type.
   *
   * @param value The value to be tested.
   * @return true, if this PartialConsumer is defined at value. false otherwise.
   */
  public boolean isDefinedAt(Object value) {
    for (TypeClause<?> c : clauses) {
      if (c.matches(value)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Add a clause to this PartialConsumer that matches a given type.
   *
   * @param <T>  The type that this clause will match.
   * @param type The Class object representing the type this clause will match.
   * @param body The consumer that will handle objects of this type.
   * @return {@code this}, so that calls to {@link #match} and {@link #otherwise} can be chained.
   */
  public <T> PartialConsumer match(Class<T> type, Lambda.One<T> body) {
    clauses.add(new TypeClause<>(type, body));
    return this;
  }

  public <T> PartialConsumer match(Class<T> type, Matcher<T> matcher, Lambda.One<T> body) {
    clauses.add(new MatcherClause<>(type, matcher, body));
    return this;
  }

  public <T> PartialConsumer match(Class<T> type, Predicate<T> predicate, Lambda.One<T> body) {
    clauses.add(new PredicateClause<>(type, predicate, body));
    return this;
  }

  /**
   * Add a clause to this PartialConsumer that matches a given type.
   *
   * @param <T>  The type that this clause will match.
   * @param type The Class object representing the type this clause will match.
   * @param body The consumer that will handle objects of this type.
   * @return {@code this}, so that calls to {@link #match} and {@link #otherwise} can be chained.
   */
  public <T> PartialConsumer exactMatch(Class<T> type, Lambda.One<T> body) {
    clauses.add(new ExactTypeClause<>(type, body));
    return this;
  }

  /**
   * Add a default clause to this PartialConsumer that will handle any object.
   * This is equivalent to {@code #match(Object.class, ...)}.
   *
   * @param body The consumer that will handle any object.
   * @return {@code this}, so that calls to {@link #match} and {@link #otherwise} can be chained.
   */
  public PartialConsumer otherwise(Lambda.One<Object> body) {
    return match(Object.class, body);
  }
}
