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
import org.jetbrains.annotations.*;
import org.slf4j.*;

/**
 * An Erlike process. This is a specialized thread with builtin functions
 * (i.e. protected final methods). User defined Procs will subclass this.
 * <p>
 * I have tried to reuse Java's own multi threading machinery when possible.
 * This class extends {@link Thread} to avoid duplicating work. Similarly, {@link Node}
 * extends {@link ThreadGroup}.
 *
 * @see ProcId
 * @see Thread
 */
public abstract class Proc extends Thread {
    private static final Logger log = LoggerFactory.getLogger(Proc.class);

    /**
     * A unique exception used to signal that a Proc is exiting.
     * We need to be able to uniquely identify this exception across a network.
     */
    private static final RuntimeException NORMAL_EXIT = new RuntimeException() {
        static final long serialVersionUID = 0xDEADL;
    };

    /** The {@link Node} for this Proc. */
    @NotNull
    private final Node node;

    /** The process id ({@link ProcId}) of this Proc. */
    @NotNull
    private final ProcId selfId;

    /** The queue of incoming messages. Messages can be of any type. */
    @NotNull
    private final Mailbox<Object> mailbox;

    /**
     * The set of linked Procs. Needs to be thread-safe.
     *
     * @see #link(ProcId)
     * @see #unlink(ProcId)
     * @see #completeLink(ProcId)
     * @see #completeUnlink(ProcId)
     */
    @NotNull
    private final Set<ProcId> links;

    /**
     * @param node The {@link Node} this Proc is running on.
     */
    public Proc(@NotNull final Node node) {
        super(node, "");

        if (node == null)
            throw new NullPointerException("Proc cannot be bound to null Node.");

        this.node = node;
        selfId = new LocalProcId(this);
        mailbox = new Mailbox<>();
        links = Collections.newSetFromMap(new ConcurrentHashMap<>());

        setName(selfId.toString());
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
    final void addMail(@NotNull final Object msg) {
        if (msg instanceof SystemMail)
            // TODO In the future this will have to handle
            // procs being configured to trap system mail.
            ((SystemMail)msg).visit(this);
        else
            if (!mailbox.offer(msg))
                assert false : "Unreachable, mailbox has no max capacity.";
    }

    /**
     * Run this Proc. Thread related exceptions are handled.
     * The parent {@link Node} is notified when the Proc quits.
     */
    @Override
    public final void run() {
        Throwable reason = NORMAL_EXIT;
        try {
            main();
        } catch (InterruptedException e) {
            log.debug("{} was interrupted.", selfId, e);
            reason = e;
        } catch (Exception e) {
            if (e == NORMAL_EXIT) {
                log.debug("{} exited.", selfId);
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
     *
     * @param reason The reason this proc exited.
     */
    private void notifyLinksAndMonitors(Throwable reason) {
        ProcId self = self();
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
     * @return The {@link ProcId} of this Proc.
     */
    @Contract(pure = true)
    @NotNull
    public final ProcId self() {
        return this.selfId;
    }

    /**
     * @return The node this proc is running on.
     */
    @NotNull
    public final NodeId node() {
        return node.getRef();
    }

    /**
     * Receive a message within the given timeout, otherwise run a timeout handler.
     *
     * @see #receive(Lambda.One, Duration)
     * @see #receive(Lambda.One)
     *
     * @throws InterruptedException If the Proc is interrupted while waiting for mail.
     *
     * @param handler A consumer for the received message.
     * @param timeout The duration to wait for a message.
     * @param timeoutHandler The action to take if the receive times out.
     */
    @Contract("!null, null, !null -> fail")
    protected final void receive(@NotNull Lambda.One<Object> handler, Duration timeout, Runnable timeoutHandler)
      throws Exception {
        if (handler == null)
            throw new NullPointerException("recieve requires a message handler.");
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

        if (mail == null && timeoutHandler != null) {
            timeoutHandler.run();
        } else if (mail != null) {
            handler.accept(mail);
        }
    }

    /**
     * Receive mail within a timeout, taking no action if the timeout expires.
     *
     * @see #receive(Lambda.One, Duration, Runnable)
     * @see #receive(Lambda.One)
     *
     * @throws InterruptedException If the Proc is interrupted while waiting for mail.
     *
     * @param handler A consumer for the received message.
     * @param timeout The duration to wait for a message.
     */
    protected final void receive(@NotNull Lambda.One<Object> handler, Duration timeout)
      throws Exception {
        receive(handler, timeout, null);
    }

    /**
     * Receive mail, blocking indefinetly.
     *
     * @see #receive(Lambda.One, Duration, Runnable)
     * @see #receive(Lambda.One, Duration)
     *
     * @throws InterruptedException If the Proc is interrupted while waiting for mail.
     *
     * @param handler A consumer for the received message.
     */
    protected final void receive(@NotNull Lambda.One<Object> handler)
      throws Exception {
        receive(handler, null, null);
    }

    /**
     * Receive a message within the given timeout, otherwise run a timeout handler.
     *
     * @see #receive(Lambda.One, Duration)
     * @see #receive(Lambda.One)
     *
     * @throws InterruptedException If the Proc is interrupted while waiting for mail.
     *
     * @param handler A consumer for the received message.
     * @param timeout The duration to wait for a message.
     * @param timeoutHandler The action to take if the receive times out.
     */
    @Contract("!null, null, !null -> fail")
    protected final void receive(@NotNull PartialConsumer handler, Duration timeout, Runnable timeoutHandler)
            throws Exception {
        if (handler == null)
            throw new NullPointerException("recieve requires a message handler.");
        if (timeout == null && timeoutHandler != null)
            throw new NullPointerException(
                    "receive requires a valid timeout if the timeoutHandler is not null");

        Object mail;
        if (timeout == null)
            mail = mailbox.takeMatch(handler::isDefinedAt);
        else if (timeout.toNanos() == 0)
            mail = mailbox.pollMatch(handler::isDefinedAt);
        else
            mail = mailbox.pollMatch(handler::isDefinedAt, timeout.toMillis(), TimeUnit.MILLISECONDS);

        if (mail == null && timeoutHandler != null) {
            timeoutHandler.run();
        } else if (mail != null) {
            handler.accept(mail);
        }
    }

    /**
     * Receive mail within a timeout, taking no action if the timeout expires.
     *
     * @see #receive(Lambda.One, Duration, Runnable)
     * @see #receive(Lambda.One)
     *
     * @throws InterruptedException If the Proc is interrupted while waiting for mail.
     *
     * @param handler A consumer for the received message.
     * @param timeout The duration to wait for a message.
     */
    protected final void receive(@NotNull PartialConsumer handler, Duration timeout)
            throws Exception {
        receive(handler, timeout, null);
    }

    /**
     * Receive mail, blocking indefinetly.
     *
     * @see #receive(Lambda.One, Duration, Runnable)
     * @see #receive(Lambda.One, Duration)
     *
     * @throws InterruptedException If the Proc is interrupted while waiting for mail.
     *
     * @param handler A consumer for the received message.
     */
    @Contract("null -> fail")
    protected final void receive(PartialConsumer handler)
            throws Exception {
        receive(handler, null, null);
    }

    /**
     * Exit the Proc immediately. This can be used to return from inside of lambdas.
     * Inside of {@link #main()}, it has the same effect as {@code return;}.
     */
    @Contract(" -> fail")
    protected final void exit() {
        throw NORMAL_EXIT;
    }

    /**
     * Create a link between this and the target Proc.
     * If a link already exists, this method has no
     * effect.
     *
     * @param other The {@link ProcId} of the Proc to be linked.
     */
    protected final void link(ProcId other) {
        completeLink(other);
        other.send(new SystemMail.Link(selfId));
    }

    /**
     * Destroy any link between this process and another.
     * If no link exists, this method has no effect.
     *
     * @param other The {@link ProcId} of the Proc to be unlinked.
     */
    protected final void unlink(ProcId other) {
        completeUnlink(other);
        other.send(new SystemMail.Unlink(selfId));
    }

    final void completeLink(ProcId other) {
        log.debug("Linked {} and {}.", this, other);
        links.add(other);
    }

    final void completeUnlink(ProcId other) {
        log.debug("Unlinked {} and {}.", this, other);
        links.remove(other);
    }
}

