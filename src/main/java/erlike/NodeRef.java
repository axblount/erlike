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

import erlike.functions.Lambda;

/**
 * All references to {@link Node}s are handled transparently through this class.
 * It does not matter if it's the current Node or one running on another server.
 */
public interface NodeRef {
  ProcRef spawn(Class<? extends Proc> procType, Object... args);

  ProcRef spawn(Class<? extends Proc> procType);

  ProcRef spawn(Lambda.Zero zero);

  <A> ProcRef spawn(Lambda.One<A> one, A a);

  <T> ProcRef spawnRecursive(Lambda.Recursive<T> rec, T t);

  <A, B> ProcRef spawn(Lambda.Two<A, B> two, A a, B b);

  <A, B, C> ProcRef spawn(Lambda.Three<A, B, C> three, A a, B b, C c);

  <A, B, C, D> ProcRef spawn(Lambda.Four<A, B, C, D> four, A a, B b, C c, D d);
}
