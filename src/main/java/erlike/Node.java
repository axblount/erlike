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

import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.util.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import org.slf4j.*;

/**
 * An Erlike Node.
 */
public class Node implements Thread.UncaughtExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(Node.class);

    /** The name of the Node. */
    private final String name;

    private final UUID uuid;

    /**
     * A map of procs by their thread ids.
     * @see Thread#getId()
     */
    private final ConcurrentMap<Long, Proc> procs;

    /**
     * A list of the uncaught exceptions.
     */
    private final List<Throwable> uncaughtExceptions;

    private final ConcurrentMap<Nid, ObjectOutputStream> nodes;

    /**
     * Create a new Node.
     *
     * @param name The Node's name.
     */
    public Node(String name) {
        this.name = name;
        this.uuid = UUID.randomUUID();
        this.procs = new ConcurrentHashMap<>();
        this.uncaughtExceptions = Collections.synchronizedList(new LinkedList<>());
        this.nodes = new ConcurrentHashMap<>();
        log.debug("Node starting: {} {}.", name, uuid);
    }

    /**
     * Get the Node's name.
     *
     * @return The Node's name.
     */
    public String getName() { return name; }

    public UUID getUUID() { return uuid; }

    public Nid getRef() { return new Nid(uuid, name, InetAddress.getLoopbackAddress()); }

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
     * Send a message to a {@link Proc} on this node.
     *
     * @param procId The id of the target {@link Proc}.
     * @param msg The message to send.
     */
    final void sendById(final long procId, final Object msg) {
        Proc proc = procs.get(procId);
        if (proc != null)
            proc.addMail(msg);
        // TODO: dead letters
    }

    /**
     * Send a message to a {@link Proc} on another node.
     *
     * @param nid A reference to the receiver {@link Node}.
     * @param procId The id of the target {@link Proc}.
     * @param msg The message to send.
     */
    final void sendById(final Nid nid, final long procId, final Object msg) {
        if (nid.getUUID() == uuid) {
            sendById(procId, msg);
        } else {
            ObjectOutputStream out = nodes.get(nid);
            if (out != null) {
                try {
                    out.writeObject(new Envelope(procId, msg));
                } catch (Exception e) {
                    nodes.remove(nid);
                }
            } // else todo: dead letters
        }
    }

    /**
     * Spawn a {@link Proc} with arguments on this node.
     * The given parameters will be passed to the first matching
     * constructor of the given type.
     *
     * @param procType The type of {@link Proc} to spawn.
     * @param args Arguments for the new {@link Proc}'s constructor.
     * @return The {@link Pid} of the spawned Proc.
     */
    public Pid spawn(Class<? extends Proc> procType, Object... args) {
        Proc proc;
        if (args == null)
            args = new Object[0];

        try {
            Class<?>[] ctorTypes = Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new);
            Constructor<? extends Proc> ctor = procType.getConstructor(ctorTypes);
            proc = ctor.newInstance(args);
        } catch (NoSuchMethodException e) {
            log.warn("No matching constructor found for {}.", procType, e);
            throw new RuntimeException("No constructor with that signature found.");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            log.warn("The constructor for {} threw an exception.", procType, cause);
            throw new RuntimeException("Proc constructor threw an error.", cause);
        } catch (InstantiationException e) {
            log.warn("Could not call constructor for {}.", procType, e);
            throw new RuntimeException("Couldn't call constructor. Is the proc abstract?");
        } catch (Exception e) {
            log.warn("Couldn't constructor proc of type {}.", procType, e);
            throw new RuntimeException("Couldn't spawn proc.", e);
        }

        Pid pid = bindAndStart(proc);
        log.debug("{} of type {} spawned.", pid, procType);
        return pid;
    }

    /**
     * Spawn a {@link Proc} with the zero argument constructor.
     *
     * @see #spawn(Class, Object...)
     *
     * @param procType The type of {@link Proc} to spawn.
     * @return The {@link Pid} of the spawned Proc.
     */
    public Pid spawn(Class<? extends Proc> procType) {
        return spawn(procType, (Object[]) null);
    }

    /**
     * Spawn an anonymous proc that takes zero arguments.
     *
     * @param zero The body of the proc.
     * @return The {@link Pid} of the spawned Proc.
     */
    public Pid spawn(Lambda.Zero zero) {
        if (zero == null)
            throw new NullPointerException();
        Proc proc = new Lambda.Anon(zero);
        return bindAndStart(proc);
    }

    /**
     * Spawn an anonymous proc that takes one argument.
     *
     * @param one The body of the proc.
     * @param a The argument.
     * @param <A> The type of the argument.
     * @return The {@link Pid} of the spawned Proc.
     */
    public <A> Pid spawn(Lambda.One<A> one, A a) {
        if (one == null)
            throw new NullPointerException();
        Proc proc = new Lambda.Anon(() -> one.accept(a));
        return bindAndStart(proc);
    }

    /**
     * Spawn a recursive proc.
     *
     * @param rec The body of the proc.
     * @param t The initial argument.
     * @param <T> The type of the argument and the body's return type.
     * @return The pid of the spawned Proc.
     */
    public <T> Pid spawnRecursive(Lambda.Recursive<T> rec, T t) {
        if (rec == null)
            throw new NullPointerException();
        Proc proc = new Lambda.Rec<>(rec, t);
        return bindAndStart(proc);
    }

    /**
     * Spawn an anonymous proc that takes two arguments.
     *
     * @param two The body of the proc.
     * @param a The first argument.
     * @param b The second argument.
     * @param <A> The type of the first argument.
     * @param <B> The type of the second argument.
     * @return The pid of the spawned Proc.
     */
    public <A, B> Pid spawn(Lambda.Two<A, B> two, A a, B b) {
        if (two == null)
            throw new NullPointerException();
        Proc proc = new Lambda.Anon(() -> two.accept(a, b));
        return bindAndStart(proc);
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
     * @return The pid of the spawned Proc.
     */
    public <A, B, C> Pid spawn(Lambda.Three<A, B, C> three, A a, B b, C c) {
        if (three == null)
            throw new NullPointerException();
        Proc proc = new Lambda.Anon(() -> three.accept(a, b, c));
        return bindAndStart(proc);
    }

    /**
     * Spawn an anonymous proc with four arguments.
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
     * @return The pid of the spawned Proc.
     */
    public <A, B, C, D> Pid spawn(Lambda.Four<A, B, C, D> four, A a, B b, C c, D d) {
        if (four == null)
            throw new NullPointerException();
        Proc proc = new Lambda.Anon(() -> four.accept(a, b, c, d));
        return bindAndStart(proc);
    }

    /**
     * Assign the given proc an id, bind it to this node, and start the proc.
     *
     * @param proc The proc to register, bind, and start.
     * @return The pid of the new proc.
     */
    private Pid bindAndStart(Proc proc) {
        Pid pid = proc.startInNode(this);
        procs.put(proc.getId(), proc);
        log.debug("{} spawned.", pid);
        return pid;
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
    Proc unsafeGetProc(Pid pid) {
        if (pid instanceof Pid) {
            return procs.get(((Pid) pid).procId);
        }
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
        log.error("Node {} got an uncaught an exception from {}!!!", name, t, e);
        uncaughtExceptions.add(e);
    }

    void registerNode(Nid ref, ObjectOutputStream outputStream) {
        nodes.put(ref, outputStream);
        log.debug("New node {} registered with {}.", ref, name);
    }
}
