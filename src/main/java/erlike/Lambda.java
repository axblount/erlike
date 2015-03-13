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

public class Lambda {
    static class Anon extends Proc {
        private final Zero main;

        Anon(final Zero lambda) {
            if (lambda == null)
                throw new NullPointerException();
            this.main = lambda;
        }

        @Override
        protected void main() throws Exception {
            main.accept(getContext());
        }
    }

    static class Rec<T> extends Proc {
        private final Recursive<T> main;
        private T arg;

        Rec(final Recursive<T> main, final T arg) {
            if (main == null || arg == null)
                throw new NullPointerException();
            this.main = main;
            this.arg = arg;
        }

        @Override
        protected void main() throws Exception {
            ProcContext ctx = getContext();
            while (arg != null)
                arg = main.apply(ctx, arg);
        }
    }

    @FunctionalInterface
    public interface Zero {
        public void accept(ProcContext ctx) throws Exception;
    }

    @FunctionalInterface
    public interface One<A> {
        public void accept(ProcContext ctx, A a) throws Exception;
    }

    @FunctionalInterface
    public interface Two<A, B> {
        public void accept(ProcContext ctx, A a, B b) throws Exception;
    }

    @FunctionalInterface
    public interface Three<A, B, C> {
        public void accept(ProcContext ctx, A a, B b, C c) throws Exception;
    }

    @FunctionalInterface
    public interface Four<A, B, C, D> {
        public void accept(ProcContext ctx, A a, B b, C c, D d) throws Exception;
    }

    @FunctionalInterface
    public interface Recursive<T> {
        public T apply(ProcContext ctx, T arg) throws Exception;
    }
}
