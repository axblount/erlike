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

/**
 * The class provides functional interfaces for anonymous procs.
 */
public final class Lambda {
  private Lambda() {}

  /**
   * Takes no arguments, returns nothing, and can throw an exception.
   */
  @FunctionalInterface
  public interface Zero {
    void run() throws Exception;
  }

  /**
   * Takes one argument, returns nothing, and can throw an exception.
   *
   * @param <A> Type of the first argument.
   */
  @FunctionalInterface
  public interface One<A> {
    void accept(A a) throws Exception;
  }

  /**
   * Takes two arguments, returns nothing, and can throw an exception.
   *
   * @param <A> Type of the first argument.
   * @param <B> Type of the second argument.
   */
  @FunctionalInterface
  public interface Two<A, B> {
    void accept(A a, B b) throws Exception;
  }

  /**
   * Takes three arguments, returns nothing, and can throw an exception.
   *
   * @param <A> Type of the first argument.
   * @param <B> Type of the second argument.
   * @param <C> Type of the third argument.
   */
  @FunctionalInterface
  public interface Three<A, B, C> {
    void accept(A a, B b, C c) throws Exception;
  }

  /**
   * Takes four arguments, returns nothing, and can throw an exception.
   * If your proc requires more than four arguments, wrap them up in an object.
   *
   * @param <A> Type of the first argument.
   * @param <B> Type of the second argument.
   * @param <C> Type of the third argument.
   * @param <D> Type of the fourth argument.
   */
  @FunctionalInterface
  public interface Four<A, B, C, D> {
    void accept(A a, B b, C c, D d) throws Exception;
  }
}
