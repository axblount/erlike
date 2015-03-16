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

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import org.slf4j.*;

/**
 * An Erlike process. This is a specialized thread with builtin functions
 * (i.e. protected final methods). User defined Procs will subclass this.
 * <p>
 * I extend Thread because I am a wild man.
 * <p>
 * TODO: Write an explanation of why extending thread is a Good Thing in this case.
 */
public abstract class Proc extends Thread {
    private static final Logger log = LoggerFactory.getLogger(Proc.class);

    /** A unique exception used to signal that a Proc is exiting. */
    private static final RuntimeException NORMAL_EXIT = new RuntimeException() {
        static final long serialVersionUID = 1L;
    };

    /** The {@link Node} this Proc is running on. */
    private Node node;

    /** The process id of this Proc. */
    private Pid pid;

    /** The queue of incoming messages. */
    private Mailbox<Object> mailbox;

    /** The set of linked Procs. Needs to be thread-safe. */
    private Set<Pid> links;

    /**
     * Bind this Proc to a {@link Node}, establishing its identity.
     * This should be called exactly once.
     *
     * @param node The {@link Node} this Proc is running on.
     * @return Pid of the bound process.
     */
    final Pid startInNode(final Node node) {
        if (node == null)
            throw new NullPointerException("Proc cannot be bound to null Node.");
        if (getState() != State.NEW)
            throw new IllegalStateException("Proc cannot be bound twice.");

        this.node = node;
        pid = new LocalPid(this.node, getId());
        mailbox = new Mailbox<>();
        links = Collections.newSetFromMap(new ConcurrentHashMap<>());

        setName(pid.toString());
        setUncaughtExceptionHandler(this.node);
        start();

        return pid;
    }

    /**
     * User defined Procs provide behavior by overriding this method.
     *
     * @throws Exception Procs are not required to catch exceptions.
     */
    protected abstract void main() throws Exception;

    /**
     * Add mail to the mailbox.
     */
    final void addMail(Object msg) {
        if (msg instanceof SystemMail)
            // TODO In the future this will have to handle
            // procs being configured to trap system mail.
            ((SystemMail)msg).visit(this);
        else
            mailbox.offer(msg);
    }

    /**
     * Run this Proc. Thread related exceptions are handled.
     * The parent {@link Node} is notified when the proc quits.
     */
    @Override
    public final void run() {
        Throwable reason = NORMAL_EXIT;
        try {
            main();
        } catch (InterruptedException e) {
            log.debug("{} was interrupted.", pid, e);
            reason = e;
        } catch (Exception e) {
            if (e == NORMAL_EXIT) {
                log.debug("{} exited.", pid);
            } else {
                reason = e;
                node.uncaughtException(this, e);
            }
        } finally {
            notifyLinksAndMonitors(reason);
            node.notifyExit(this);
        }
    }

    /**
     * Notify if necessary all linked and monitoring processes that this process has exited.
     * @param reason The reason this proc exited.
     */
    private void notifyLinksAndMonitors(Throwable reason) {
        Pid self = self();
        if (reason != NORMAL_EXIT) {
            // Only notify links if the exit wasn't normal.
            log.debug("{} notifying links of abnormal exit.", self, reason);
            links.forEach(pid ->
                    pid.send(new SystemMail.LinkExit(self)));
        }
        // TODO: notify monitors
    }

    /*=====================*/
    /*  Library Functions  */
    /*=====================*/

    /**
     * @return The {@link Pid} of this Proc.
     */
    public final Pid self() {
        return this.pid;
    }

    /**
     * @return The node this proc is running on.
     */
    public final Node node() {
        return node;
    }

    /**
     * Receive a message within the given timeout, otherwise run a timeout handler.
     *
     * @see #receive(Consumer, Duration)
     * @see #receive(Consumer)
     *
     * @throws InterruptedException If the Proc is interrupted while waiting for mail.
     *
     * @param handler A consumer for the received message.
     * @param timeout The duration to wait for a message.
     * @param timeoutHandler The action to take if the receive times out.
     */
    protected final void receive(Consumer<Object> handler, Duration timeout, Runnable timeoutHandler)
      throws InterruptedException {
        if (handler == null)
            throw new NullPointerException("recieve requires a non-null message handler.");
        if (timeout == null && timeoutHandler != null)
            throw new NullPointerException(
                    "receive requires a valid timeout if the timeoutHandler is not null");

        Object mail;
        if (timeout == null)
            mail = mailbox.take();
        else if (timeout.toNanos() == 0)
            mail = mailbox.poll();
        else
            mail = mailbox.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);

        if (mail == null && timeoutHandler != null)
            timeoutHandler.run();

        handler.accept(mail);
    }

    /**
     * Receive mail within a timeout, taking no action if the timeout expires.
     *
     * @see #receive(Consumer, Duration, Runnable)
     * @see #receive(Consumer)
     *
     * @throws InterruptedException If the Proc is interrupted while waiting for mail.
     *
     * @param handler A consumer for the received message.
     * @param timeout The duration to wait for a message.
     */
    protected final void receive(Consumer<Object> handler, Duration timeout)
      throws InterruptedException {
        receive(handler, timeout, null);
    }

    /**
     * Receive mail, blocking indefinetly.
     *
     * @see #receive(Consumer, Duration, Runnable)
     * @see #receive(Consumer, Duration)
     *
     * @throws InterruptedException If the Proc is interrupted while waiting for mail.
     *
     * @param handler A consumer for the received message.
     */
    protected final void receive(Consumer<Object> handler)
      throws InterruptedException {
        receive(handler, null, null);
    }

    /**
     * Exit the Proc immediately. This can be used to return from inside of lambdas.
     * Inside main, it has the same effect as {@code return}.
     */
    protected final void exit() {
        throw NORMAL_EXIT;
    }

    /**
     * Create a link between this and the target process.
     * If a link already exists, this method has no
     * effect.
     *
     * @param other The pid to link.
     */
    protected final void link(Pid other) {
        links.add(other);
        other.send(new SystemMail.Link(self()));
    }

    /**
     * Destroy any link between this process and another.
     * If no link exists, this method has no effect.
     *
     * @param other The pid to link.
     */
    protected final void unlink(Pid other) {
        links.remove(other);
        other.send(new SystemMail.Unlink(self()));
    }

    final void completeLink(Pid other) {
        links.add(other);
    }

    final void completeUnlink(Pid other) {
        links.remove(other);
    }
}

