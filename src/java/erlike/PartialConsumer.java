package erlike;

import java.util.List;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * Implements
 */
public class PartialConsumer implements Consumer<Object> {

    private static class Clause<T> implements Consumer<T> {
        private Class<T> type;
        private Consumer<T> body;

        Clause(Class<T> type, Consumer<T> body) {
            this.type = type;
            this.body = body;
        }

        public boolean matches(Object obj) {
            return type.isInstance(obj);
        }

        @Override public void accept(T arg) {
            body.accept(arg);
        }
    }

    private List<Clause<?>> clauses;

    public PartialConsumer() { clauses = new LinkedList<>(); }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addClause(Class type, Consumer body) {
        clauses.add(new Clause(type, body));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override public void accept(Object arg) {
        for (Clause c : clauses) {
            if (c.matches(arg)) {
                c.accept(arg);
                return;
            }
        }

        throw new IllegalArgumentException("Partial function not defined at value: " + arg);
    }

    public boolean isDefinedAt(Object value) {
        for (Clause<?> c : clauses)
            if (c.matches(value))
                return true;
        return false;
    }

    public <T> PartialConsumer match(Class<T> type, Consumer<T> body) {
        clauses.add(new Clause<T>(type, body));
        return this;
    }

    public PartialConsumer otherwise(Consumer<Object> body) {
        return match(Object.class, body);
    }

    /*
     * This doesnt work how I want it to. 
     * I believe it's due to a limitation in the java compiler.
     * You cannot pass a lambda into an Object parameter without casting it first.
     * Even if the lambda has type annotations.
     * "Object is not a functional interface"
     * This greatly reduces the utility of #build.
     * Here is a short example.
     
        import java.util.function.DoubleConsumer;
        import java.util.function.IntConsumer;

        public class Test {
            static void testFunc(Object obj) {
                try {
                    IntConsumer f = (IntConsumer) obj;
                    f.accept(1);
                } catch (ClassCastException e) { }

                try {
                    DoubleConsumer f = (DoubleConsumer) obj;
                    f.accept(2);
                } catch (ClassCastException e) { }
            }

            public static void main(String[] args) {
                // won't compile:
                //      testFunc(x -> System.out.println(x));
                testFunc((DoubleConsumer) x -> System.out.println(x));
            }
        }
     */
    @SuppressWarnings("unchecked") 
    public static PartialConsumer build(Object... args) {
        PartialConsumer f = new PartialConsumer();
        int len = args.length;
        try {
            for (int i = 0; i < len; i += 2) {
                if (i + 1 >= len) {
                    if (args[i] instanceof Consumer) {
                        f.addClause(Object.class, /*unchecked*/(Consumer<Object>)args[i]);
                        return f;
                    } else {
                        throw new IllegalArgumentException("Expected a Consumer<Object> as the last argument.");
                    }
                } else {
                    f.addClause((Class<?>)args[i], (Consumer<?>)args[i+1]);
                }
            }

            return f;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Couldn't construct partial function.", e);
        }
    }

}
