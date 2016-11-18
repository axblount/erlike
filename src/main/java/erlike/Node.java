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

import java.util.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import org.slf4j.*;

/**
 * A Node represents a group of {@link Proc}s. The node keeps track of all running
 * {@link Proc}s and managing communication with other Nodes. In plain ol' Java terms:
 * <code>
 *     {@link Node} : {@link ThreadGroup} :: {@link Proc} : {@link Thread}
 * </code>
 *
 * Node objects should only be handled at the top-level of your program. Inside of {@link Proc}s
 * Nodes should be referenced by {@link NodeId}s. You can get the {@link NodeId} for a Node with
 * {@link #getRef()}.
 *
 * @see NodeId
 * @see ThreadGroup
 */
public class Node extends ThreadGroup {
    private static final Logger log = LoggerFactory.getLogger(Node.class);

    private final NodeId selfId;

    /**
     * A map of procs by their thread ids.
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
    public Node(String name) {
        super(name);
        this.selfId = new LocalNodeId(this);
        this.procs = new ConcurrentHashMap<>();
        this.uncaughtExceptions = Collections.synchronizedList(new LinkedList<>());
        log.debug("Node starting: {}.", name);
    }

    public NodeId getRef() { return selfId; }

    /**
     * Get all uncaughtExceptions. The caller is free to
     * remove exceptions or clear the list.
     *
     * @return A list of uncaught exceptions.
     */
    public List<Throwable> getUncaughtExceptions() {
        return uncaughtExceptions;
    }

    /**
     * Join all currently running {@link Proc}s.
     *
     * @throws InterruptedException If the thread is interrupted while waiting.
     */
    public void joinAll() throws InterruptedException {
        for (Proc p : procs.values())
            p.join();
    }

    /**
     * Send a message to a {@link Proc} on another node.
     *
     * @param pid The {@link ProcId} of the target {@link Proc}.
     * @param msg The message to send.
     */
    // todo: dead letters
    final void send(final ProcId pid, final Object msg) {
        NodeId nid = pid.node();
        if (nid.equals(getRef())) {
            Proc proc = procs.get(pid.id());
            if (proc != null)
                proc.addMail(msg);
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
     * @param args Arguments for the new {@link Proc}'s constructor.
     * @return The {@link ProcId} of the spawned Proc.
     */
    public ProcId spawn(Class<? extends Proc> procType, Object... args) {
        Proc proc;
        if (args == null)
            args = new Object[0];

        try {
            Class<?>[] ctorTypes = Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new);
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
            log.error("Couldn't constructor proc of type {}.", procType, e);
            throw new RuntimeException("Couldn't spawn proc.", e);
        }

        proc.start();
        log.debug("{} (type: {}) spawned in {}", proc, procType, this);
        return proc.self();
    }

    /**
     * Spawn a {@link Proc} with the zero argument constructor.
     *
     * @see #spawn(Class, Object...)
     *
     * @param procType The type of {@link Proc} to spawn.
     * @return The {@link ProcId} of the spawned Proc.
     */
    public ProcId spawn(Class<? extends Proc> procType) {
        return spawn(procType, (Object[]) null);
    }

    /**
     * Spawn an anonymous proc that takes zero arguments.
     *
     * @param zero The body of the proc.
     * @return The {@link ProcId} of the spawned Proc.
     */
    public ProcId spawn(Lambda.Zero zero) {
        if (zero == null)
            throw new NullPointerException();
        Proc proc = new Lambda.Anon(this, zero);
        procs.put(proc.getId(), proc);
        proc.start();
        log.debug("{} spawned in {}", proc, this);
        return proc.self();
    }

    /**
     * Spawn an anonymous proc that takes one argument.
     *
     * @param one The body of the proc.
     * @param a The argument.
     * @param <A> The type of the argument.
     * @return The {@link ProcId} of the spawned Proc.
     */
    public <A> ProcId spawn(Lambda.One<A> one, A a) {
        return spawn(() -> one.accept(a));
    }

    /**
     * Spawn a recursive proc.
     *
     * @param rec The body of the proc.
     * @param t The initial argument.
     * @param <T> The type of the argument and the body's return type.
     * @return The ProcId of the spawned Proc.
     */
    public <T> ProcId spawnRecursive(Lambda.Recursive<T> rec, T t) {
        if (rec == null)
            throw new NullPointerException();
        Proc proc = new Lambda.Rec<>(this, rec, t);
        procs.put(proc.getId(), proc);
        proc.start();
        log.debug("{} spawned in {}", proc, this);
        return proc.self();
    }

    /**
     * Spawn an anonymous proc that takes two arguments.
     *
     * @param two The body of the proc.
     * @param a The first argument.
     * @param b The second argument.
     * @param <A> The type of the first argument.
     * @param <B> The type of the second argument.
     * @return The ProcId of the spawned Proc.
     */
    public <A, B> ProcId spawn(Lambda.Two<A, B> two, A a, B b) {
        return spawn(() -> two.accept(a, b));
    }

    /**
     * Spawn an anonymous proc that takes three arguments.
     *
     * @param three The body of the proc.
     * @param a The first argument.
     * @param b The second argument.
     * @param c The third argument.
     * @param <A> The type of the first argument.
     * @param <B> The type of the second argument.
     * @param <C> The type of the third argument.
     * @return The ProcId of the spawned Proc.
     */
    public <A, B, C> ProcId spawn(Lambda.Three<A, B, C> three, A a, B b, C c) {
        return spawn(() -> three.accept(a, b, c));
    }

    /**
     * Spawn an anonymous proc with four arguments. If you have a proc that requires
     * more than four arguments, you should wrap them up in an object.
     *
     * @param four The body of the proc.
     * @param a The first argument.
     * @param b The second argument.
     * @param c The third argument.
     * @param d The fourth argument.
     * @param <A> The type of the first argument.
     * @param <B> The type of the second argument.
     * @param <C> The type of the third argument.
     * @param <D> The type of the fourth argument.
     * @return The ProcId of the spawned Proc.
     */
    public <A, B, C, D> ProcId spawn(Lambda.Four<A, B, C, D> four, A a, B b, C c, D d) {
        return spawn(() -> four.accept(a, b, c, d));
    }

    /**
     * Used by {@link Proc}s to notify the parent node that they have exited.
     *
     * @param proc The {@link Proc} that has exited.
     */
    void notifyExit(Proc proc) {
        log.debug("Proc {} exited.", proc.getId());
        if (!procs.remove(proc.getId(), proc)) {
            log.error("Tried to remove {} after exit, but failed!", proc);
            log.error("(Was proc in #procs?)={}", procs.containsValue(proc));
        }
    }

    /**
     * Used for testing only!!!
     */
    Proc unsafeGetProc(ProcId pid) {
        if (pid != null)
            return procs.get(pid.id());
        return null;
    }

    /**
     * Handle an uncaught exception thrown by a {@link Proc} running on this node. If this gets
     * called, something has gone very wrong.
     *
     * @param t The {@link Thread} that threw the exception. This should be an instance of {@link Proc}.
     * @param e The exception thrown by the {@link Thread}.
     */
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        log.error("Node {} got an uncaught an exception from {}!!!", getName(), t, e);
        uncaughtExceptions.add(e);
    }
}
