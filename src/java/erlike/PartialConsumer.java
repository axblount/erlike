package erlike;

import java.util.List;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * A PartialConsumer is a Consumer that is only defined for certain types.
 */
public class PartialConsumer implements Consumer<Object> {

    private static class Clause<T> {
        private final Class<T> type;
        private final Consumer<T> body;

        Clause(Class<T> type, Consumer<T> body) {
            this.type = type;
            this.body = body;
        }

        public boolean matches(Object obj) {
            return type.isInstance(obj);
        }

        public void accept(Object arg) {
            body.accept(type.cast(arg));
        }
    }

    private List<Clause<?>> clauses;

    /** Create a new PartialConsumer that matches no objects. */
    public PartialConsumer() { clauses = new LinkedList<>(); }

    /**
     * TODO
     */
    @Override
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
        clauses.add(new Clause<T>(type, body));
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
