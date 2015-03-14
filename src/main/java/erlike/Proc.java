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
import java.util.concurrent.*;
import java.util.function.*;
import org.slf4j.*;

/**
 * An Erlike process. This is a specialized thread with builtin functions
 * (i.e. protected final methods). User defined Procs will subclass this.
 *
 * It extends Thread because I am a wild man.
 */
public abstract class Proc extends Thread {
    private static final Logger log = LoggerFactory.getLogger(Proc.class);

    /** A unique exception used to signal that a Proc is exiting. */
    private static final RuntimeException EXIT = new RuntimeException() {
        static final long serialVersionUID = 1L;
    };

    /** The {@link Node} this Proc is running on. */
    private Node homeNode;

    /** The process id of this Proc. */
    private Pid pid;

    /** The queue of incoming messages. */
    private Mailbox<Object> mailbox;

    /**
     * Bind this Proc to a {@link Node}, establishing its identity.
     * This should be called exactly once.
     *
     * @param homeNode The {@link Node} this Proc is running on.
     * @return Pid of the bound process.
     */
    final Pid startInNode(final Node homeNode) {
        if (homeNode == null)
            throw new NullPointerException("Proc cannot be bound to null Node.");
        if (getState() != State.NEW)
            throw new IllegalStateException("Proc cannot be bound twice.");

        this.homeNode = homeNode;
        pid = new LocalPid(this.homeNode, getId());
        mailbox = new Mailbox<>();

        setName(pid.toString());
        setUncaughtExceptionHandler(this.homeNode);
        start();

        return this.pid;
    }

    /**
     * User defined Procs provide behavior by overriding this method.
     *
     * @throws Exception Procs are allowed to throw exceptions.
     */
    protected abstract void main() throws Exception;

    /**
     * Add mail to the mailbox.
     */

    final void addMail(Object msg) {
        mailbox.offer(msg);
    }

    /**
     * Run this Proc. Thread related exceptions are handled.
     * The parent {@link Node} is notified when the proc quits.
     */
    @Override
    public final void run() {
        try {
            main();
        } catch (InterruptedException e) {
            log.debug("{} interrupted.", pid, e);
        } catch (Exception e) {
            if (e == EXIT)
                log.debug("{} exited.", pid);
            else
                log.warn("{} threw an unexpected exception.", pid, e);
        } finally {
            homeNode.notifyExit(this);
        }
    }

    /**
     * Check a piece of mail for any system action that needs to be taken.
     *
     * @param mail The mail to be checked.
     */
    private void checkMail(Object mail) {
        if (mail instanceof SystemMail)
            ((SystemMail)mail).visit(this);
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
        return homeNode;
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

        checkMail(mail);
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
        throw EXIT;
    }
}

