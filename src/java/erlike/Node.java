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

import java.util.Arrays;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An Erlike Node.
 */
public class Node implements Thread.UncaughtExceptionHandler {
    /** This is the node id used by {@link Proc}s to identify their parent Node. */
    public static final int SELF_NODE_ID = 0;

    /** The name of the Node. */
    private final String name;

    /** The next node id to be assigned. */
    private final AtomicInteger nextNodeId;

    /** A {@link Registry} of all running {@link Proc}s. */
    private final Registry<Proc> procs;

    /**
     * Create a new Node.
     *
     * @param name The Node's name.
     */
    public Node(String name) {
        this.nextNodeId = new AtomicInteger(SELF_NODE_ID + 1);
        this.name = name;
        this.procs = new Registry<Proc>(1000);
    }

    /**
     * Get the Node's name.
     *
     * @return The Node's name.
     */
    public String getName() { return name; }

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
    /*package-local*/
    final void sendById(final int procId, final Object msg) {
        Proc proc = procs.get(procId);
        if (proc != null)
            proc.addMail(msg);
    }

    /**
     * Send a message to a {@link Proc} on another node.
     *
     * @param nodeId The id of the target {@link Proc}'s node.
     * @param procId The id of the target {@link Proc}.
     * @param msg The message to send.
     */
    /*package-local*/
    final void sendById(final int nodeId, final int procId, final Object msg) {
        if (nodeId == SELF_NODE_ID)
            sendById(procId, msg);
        // TODO
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
            Class<?>[] ctorTypes = Arrays.stream(args).map(a -> a.getClass()).toArray(Class<?>[]::new);
            Constructor<? extends Proc> ctor = procType.getConstructor(ctorTypes);
            proc = ctor.newInstance(args);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                "No constructor with that signature found.");
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Proc constructor threw an error.", e.getCause());
        } catch (InstantiationException e) {
            throw new RuntimeException("Couldn't call constructor. Is the proc abstract?");
        } catch (Exception e) {
            throw new RuntimeException("Couldn't spawn proc.", e);
        }

        int procId = procs.register(proc);
        Pid pid = new PidImpl(this, SELF_NODE_ID, procId);
        proc.bindAndStart(this, pid);
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
        return spawn(procType, (Object[])null);
    }

    /**
     * Used by {@link Proc}s to notify the parent node that they have exited.
     *
     * @param proc The {@link Proc} that has exited.
     */
    /*package-local*/
    void notifyExit(Proc proc) {
        procs.removeValue(proc);
    }

    /**
     * Handle an uncaught exception thrown by a {@link Proc} running on this node.
     *
     * @param t The {@link Thread} that threw the exception. This should be an instance of {@link Proc}.
     * @param e The exception thrown by the {@link Thread}.
     */
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (t instanceof Proc) { // it better be
            Proc p = (Proc) t;
            notifyExit(p); // TODO should be failure
        }
    }
}
