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

/**
 * The class provides functional interfaces for anonymous procs.
 */
public class Lambda {
    /**
     * An anonymous proc that uses a user provided lambda
     * as its lambda function.
     */
    static class Anon extends Proc {
        private final Zero lambda;

        Anon(final Node node, final Zero lambda) {
            super(node);
            if (lambda == null)
                throw new NullPointerException();
            this.lambda = lambda;
        }

        @Override
        protected void main() throws Exception {
            lambda.run();
        }
    }

    /**
     * A recursive proc.
     *
     * @param <T> The type of the recursive argument.
     */
    static class Rec<T> extends Proc {
        private final Recursive<T> lambda;
        private final T initArg;

        Rec(Node node, final Recursive<T> lambda, final T initArg) {
            super(node);
            if (lambda == null || initArg == null)
                throw new NullPointerException();
            this.lambda = lambda;
            this.initArg = initArg;
        }

        @Override
        protected void main() throws Exception {
            T arg = initArg;
            while (arg != null)
                arg = lambda.apply(arg);
        }
    }

    @FunctionalInterface
    public interface Zero {
        void run() throws Exception;
    }

    @FunctionalInterface
    public interface One<A> {
        void accept(A a) throws Exception;
    }

    @FunctionalInterface
    public interface Two<A, B> {
        void accept(A a, B b) throws Exception;
    }

    @FunctionalInterface
    public interface Three<A, B, C> {
        void accept(A a, B b, C c) throws Exception;
    }

    @FunctionalInterface
    public interface Four<A, B, C, D> {
        void accept(A a, B b, C c, D d) throws Exception;
    }

    @FunctionalInterface
    public interface Recursive<T> {
        T apply(T arg) throws Exception;
    }
}
