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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import erlike.functions.Lambda;

import static java.util.Objects.requireNonNull;

/**
 * A Node represents a group of {@link Proc}s. The node keeps track of all running
 * {@link Proc}s and managing communication with other Nodes. In plain ol' Java terms:
 * <p>
 * <code>
 * {@link Node} : {@link ThreadGroup} :: {@link Proc} : {@link Thread}
 * </code>
 * <p>
 * Node objects should only be handled at the top-level of your program. Inside of {@link Proc}s
 * Nodes should be referenced by {@link NodeRef}s. You can get the {@link NodeRef} for a Node with
 * {@link #self()}.
 *
 * @see NodeRef
 * @see ThreadGroup
 */
public class Node extends ThreadGroup {
  private static final Logger log = LoggerFactory.getLogger(Node.class);

  private final NodeRef selfNodeRef;

  /**
   * A map of procs by their thread ids.
   *
   * @see Thread#getId()
   */
  private final ConcurrentMap<Long, Proc> procs;

  /**
   * A list of the uncaught exceptions.
   */
  private final List<Throwable> uncaughtExceptions;

  /**
   * Create a new Node.
   *
   * @param name The Node's name.
   */
  public Node(final String name) {
    super(name);
    this.selfNodeRef = new LocalNodeRef(this);
    this.procs = new ConcurrentHashMap<>();
    this.uncaughtExceptions = Collections.synchronizedList(new LinkedList<>());
    log.debug("Node starting: {}.", name);
  }

  /**
   * Get a {@link NodeRef} to this Node.
   */
  public NodeRef self() {
    return selfNodeRef;
  }

  /**
   * Get all uncaught exceptions throw by {@link Proc}s running on this node.
   * It is thread-safe for the caller to remove exceptions or clear the list.
   *
   * @return A list of uncaught exceptions thrown by {@link Proc}s on this Node.
   */
  public List<Throwable> getUncaughtExceptions() {
    return uncaughtExceptions;
  }

  /**
   * Join all currently running {@link Proc}s.
   *
   * @throws InterruptedException If the thread is interrupted while waiting.
   * @see Thread#join()
   */
  public void joinAll() throws InterruptedException {
    for (Proc p : procs.values()) {
      p.join();
    }
  }

  /**
   * Send a message to a {@link Proc} on another node.
   *
   * @param pid The {@link ProcRef} of the target {@link Proc}.
   * @param msg The message to send.
   */
  // todo: dead letters
  final void send(final ProcRef pid, final Object msg) {
    NodeRef nid = pid.node();
    if (nid.equals(self())) {
      Proc proc = procs.get(pid.id());
      if (proc != null) {
        proc.addMail(msg);
      }
    } else {
      log.error("Out of node messaging not yet supported.");
      throw new RuntimeException("Out of node message.");
    }
  }

  /**
   * Spawn a {@link Proc} with arguments on this node.
   * The given parameters will be passed to the first matching
   * constructor of the given type.
   *
   * @param procType The type of {@link Proc} to spawn.
   * @param args     Arguments for the new {@link Proc}'s constructor.
   * @return The {@link ProcRef} of the spawned Proc.
   */
  public ProcRef spawn(Class<? extends Proc> procType, Object... args) {
    requireNonNull(procType, "You must provide a valid Class object to spawn.");

    Proc proc;

    try {
      // This was the original but checkstyle couldn't handle "Class<?>[]::new"
      // Class<?>[] ctorTypes = Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new);
      Class<?>[] ctorTypes = new Class<?>[args.length];
      for (int i = 0; i < args.length; i++) {
        requireNonNull(args[i], "You cannot pass null arguments to spawn.");
        ctorTypes[i] = args[i].getClass();
      }
      Constructor<? extends Proc> ctor = procType.getConstructor(ctorTypes);
      proc = ctor.newInstance(args);
    } catch (NoSuchMethodException e) {
      log.error("No matching constructor found for {}.", procType, e);
      throw new RuntimeException("No constructor with that signature found.");
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      log.error("The constructor for {} threw an exception.", procType, cause);
      throw new RuntimeException("Proc constructor threw an error.", cause);
    } catch (InstantiationException e) {
      log.error("Could not call constructor for {}.", procType, e);
      throw new RuntimeException("Couldn't call constructor. Is the proc abstract?");
    } catch (Exception e) {
      log.error("Couldn't construct proc of type {}.", procType, e);
      throw new RuntimeException("Couldn't spawn proc.", e);
    }

    proc.start();
    log.debug("{} (type: {}) spawned in {}", proc, procType, this);
    return proc.self();
  }

  /**
   * Spawn an anonymous proc that takes zero arguments.
   *
   * @param zero The body of the proc.
   * @return The {@link ProcRef} of the spawned Proc.
   */
  public ProcRef spawn(final Lambda.Zero zero) {
    Proc proc = new Proc(this) {
      @Override
      protected void main() throws Exception {
        zero.run();
      }
    };
    procs.put(proc.getId(), proc);
    proc.start();
    log.debug("{} spawned in {}", proc, this);
    return proc.self();
  }

  /**
   * Spawn a recursive proc.
   *
   * @param rec The body of the proc.
   * @param t   The initial argument.
   * @param <T> The type of the argument and the body's return type.
   * @return The ProcRef of the spawned Proc.
   */
  public <T> ProcRef spawnRecursive(Lambda.Recursive<T> rec, T t) {
    Proc proc = new Lambda.Rec<>(this, requireNonNull(rec), requireNonNull(t));
    procs.put(proc.getId(), proc);
    proc.start();
    log.debug("{} spawned in {}", proc, this);
    return proc.self();
  }

  /**
   * Spawn an anonymous proc that takes one argument.
   *
   * @param one The body of the proc.
   * @param a   The argument.
   * @param <A> The type of the argument.
   * @return The {@link ProcRef} of the spawned Proc.
   */
  public <A> ProcRef spawn(Lambda.One<A> one, A a) {
    requireNonNull(one);

    return spawn(() -> one.accept(a));
  }

  /**
   * Spawn an anonymous proc that takes two arguments.
   *
   * @param two The body of the proc.
   * @param a   The first argument.
   * @param b   The second argument.
   * @param <A> The type of the first argument.
   * @param <B> The type of the second argument.
   * @return The ProcRef of the spawned Proc.
   */
  public <A, B> ProcRef spawn(Lambda.Two<A, B> two, A a, B b) {
    requireNonNull(two);

    return spawn(() -> two.accept(a, b));
  }

  /**
   * Spawn an anonymous proc that takes three arguments.
   *
   * @param three The body of the proc.
   * @param a     The first argument.
   * @param b     The second argument.
   * @param c     The third argument.
   * @param <A>   The type of the first argument.
   * @param <B>   The type of the second argument.
   * @param <C>   The type of the third argument.
   * @return The ProcRef of the spawned Proc.
   */
  public <A, B, C> ProcRef spawn(Lambda.Three<A, B, C> three, A a, B b, C c) {
    requireNonNull(three);

    return spawn(() -> three.accept(a, b, c));
  }

  /**
   * Spawn an anonymous proc with four arguments. If you have a proc that requires
   * more than four arguments, you should wrap them up in an object.
   *
   * @param four The body of the proc.
   * @param a    The first argument.
   * @param b    The second argument.
   * @param c    The third argument.
   * @param d    The fourth argument.
   * @param <A>  The type of the first argument.
   * @param <B>  The type of the second argument.
   * @param <C>  The type of the third argument.
   * @param <D>  The type of the fourth argument.
   * @return The ProcRef of the spawned Proc.
   */
  public <A, B, C, D> ProcRef spawn(Lambda.Four<A, B, C, D> four, A a, B b, C c, D d) {
    requireNonNull(four);

    return spawn(() -> four.accept(a, b, c, d));
  }

  /**
   * Used by {@link Proc}s to notify the parent node that they have exited.
   *
   * @param proc The {@link Proc} that has exited.
   */
  void notifyExit(Proc proc) {
    requireNonNull(proc);

    log.debug("Proc {} exited.", proc.getId());
    if (!procs.remove(proc.getId(), proc)) {
      log.error("Tried to remove {} after exit, but failed!", proc);
      log.error("(Was proc in #procs?)={}", procs.containsValue(proc));
    }
  }

  /**
   * Used for testing only!!!
   */
  Proc unsafeGetProc(ProcRef pid) {
    return procs.get(requireNonNull(pid).id());
  }

  /**
   * Handle an uncaught exception thrown by a {@link Proc} running on this node. If this gets
   * called, something has gone very wrong.
   *
   * @param t The {@link Thread} that threw the exception. This should be an instance of {@link
   *          Proc}.
   * @param e The exception thrown by the {@link Thread}.
   */
  @Override
  public void uncaughtException(Thread t, Throwable e) {
    log.error("Node {} got an uncaught an exception from {}!!!", getName(), t, e);
    uncaughtExceptions.add(e);
  }
}
