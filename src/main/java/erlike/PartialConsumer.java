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
package erlike;

import java.util.List;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * A PartialConsumer is a Consumer that is only defined for certain types.
 */
public class PartialConsumer {
    /**
     * A clause that matches instances of {@code T}.
     * @param <T> The base type this clause matches.
     */
    private static class Clause<T> {
        protected final Class<T> type;
        protected final Consumer<T> body;

        Clause(Class<T> type, Consumer<T> body) {
            this.type = type;
            this.body = body;
        }

        public boolean matches(Object arg) {
            return type.isInstance(arg);
        }

        public void accept(Object arg) {
            body.accept(type.cast(arg));
        }
    }

    /**
     * A clause that matches a type, but none of it's subtypes.
     *
     * @param <T> The exact type to match.
     */
    private static class ExactClause<T> extends Clause<T> {
        ExactClause(Class<T> type, Consumer<T> body) {
            super(type,body);
        }

        @Override
        public boolean matches(Object arg) {
            return type.equals(arg.getClass());
        }
    }

    private List<Clause<?>> clauses;

    /** Create a new PartialConsumer that matches no objects. */
    public PartialConsumer() { clauses = new LinkedList<>(); }

    public void accept(Object arg) {
        for (Clause<?> c : clauses) {
            if (c.matches(arg)) {
                c.accept(arg);
                return;
            }
        }

        throw new IllegalArgumentException("Partial function not defined at " + arg.toString());
    }

    /**
     * Determine if the PartialConsumer is defined at a given value, according to its type.
     *
     * @param value The value to be tested.
     * @return true, if this PartialConsumer is defined at value. false otherwise.
     */
    public boolean isDefinedAt(Object value) {
        for (Clause<?> c : clauses)
            if (c.matches(value))
                return true;
        return false;
    }

    /**
     * Add a clause to this PartialConsumer that matches a given type.
     *
     * @param <T> The type that this clause will match.
     * @param type The Class object representing the type this clause will match.
     * @param body The consumer that will handle objects of this type.
     * @return {@code this}, so that calls to {@link #match} and {@link #otherwise} can be chained.
     */
    public <T> PartialConsumer match(Class<T> type, Consumer<T> body) {
        clauses.add(new Clause<>(type, body));
        return this;
    }

    /**
     * Add a clause to this PartialConsumer that matches a given type.
     *
     * @param <T> The type that this clause will match.
     * @param type The Class object representing the type this clause will match.
     * @param body The consumer that will handle objects of this type.
     * @return {@code this}, so that calls to {@link #match} and {@link #otherwise} can be chained.
     */
    public <T> PartialConsumer exactMatch(Class<T> type, Consumer<T> body) {
        clauses.add(new ExactClause<>(type, body));
        return this;
    }

    /**
     * Add a default clause to this PartialConsumer that will handle any object.
     * This is equivalent to {@code #match(Object.class, ...)}.
     *
     * @param body The consumer that will handle any object.
     * @return {@code this}, so that calls to {@link #match} and {@link #otherwise} can be chained.
     */
    public PartialConsumer otherwise(Consumer<Object> body) {
        return match(Object.class, body);
    }
}
