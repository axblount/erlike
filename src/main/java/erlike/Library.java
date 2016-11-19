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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import erlike.functions.Lambda;
import erlike.functions.PartialConsumer;

/**
 * This class is statically imported to access builtin {@link Proc} functions
 * ({@link Proc#node()}, {@link Proc#self()}, etc.) in anonymous Procs.
 * See the library methods in Proc for their usage.
 * <p>
 * Calling any of these methods outside of a running Proc will cause
 * a {@link java.lang.IllegalStateException}.
 */
public final class Library {
  private static final Logger log = LoggerFactory.getLogger(Library.class);

  // static class
  private Library() {
  }

  /**
   * Get the Proc executing this method.
   *
   * @return The current thread as a Proc.
   * @throws java.lang.IllegalStateException If the current thread is not a Proc.
   */
  private static Proc currentProc() {
    final Thread t = Thread.currentThread();
    if (t instanceof Proc) {
      return (Proc) t;
    } else {
      log.error("Non-Proc thread {} attempted to use Erlike Library.", t);
      throw new IllegalStateException("Cannot call Proc Library functions from outside a Proc.");
    }
  }

  /**
   * @return The process id of the current Proc.
   * @see Proc#self()
   */
  public static ProcRef self() {
    return currentProc().self();
  }

  /**
   * @return The node the current proc is running on.
   * @see Proc#node()
   */
  public static NodeRef node() {
    return currentProc().node();
  }

  /**
   * @see Proc#receive(Lambda.One, Duration, Runnable)
   */
  public static void receive(Lambda.One<Object> handler, Duration timeout, Runnable timeoutHandler)
      throws Exception {
    currentProc().receive(handler, timeout, timeoutHandler);
  }

  /**
   * @see Proc#receive(Lambda.One, Duration)
   */
  public static void receive(Lambda.One<Object> handler, Duration timeout)
      throws Exception {
    receive(handler, timeout, null);
  }

  /**
   * @see Proc#receive(Lambda.One)
   */
  public static void receive(Lambda.One<Object> handler)
      throws Exception {
    receive(handler, null, null);
  }

  /**
   * @see Proc#receive(PartialConsumer, Duration, Runnable)
   */
  public static void receive(PartialConsumer handler, Duration timeout, Runnable timeoutHandler)
      throws Exception {
    currentProc().receive(handler, timeout, timeoutHandler);
  }

  /**
   * @see Proc#receive(PartialConsumer, Duration)
   */
  public static void receive(PartialConsumer handler, Duration timeout)
      throws Exception {
    receive(handler, timeout, null);
  }

  /**
   * @see Proc#receive(PartialConsumer)
   */
  public static void receive(PartialConsumer handler)
      throws Exception {
    receive(handler, null, null);
  }

  /**
   * @see Proc#exit()
   */
  public static void exit() {
    currentProc().exit();
  }

  /**
   * @see Proc#link(ProcRef)
   */
  public static void link(ProcRef other) {
    currentProc().link(other);
  }

  /**
   * @see Proc#unlink(ProcRef)
   */
  public static void unlink(ProcRef other) {
    currentProc().unlink(other);
  }
}
