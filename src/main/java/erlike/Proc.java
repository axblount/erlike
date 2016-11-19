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
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import erlike.functions.Lambda;
import erlike.functions.PartialConsumer;

import static java.util.Objects.requireNonNull;

/**
 * An Erlike process. This is a specialized thread with builtin functions
 * (i.e. protected final methods). User-defined class-based Procs will subclass this.
 * <p>
 * I have tried to reuse Java's own multi threading machinery when possible.
 * This class extends {@link Thread} to avoid duplicating work. Similarly, {@link Node}
 * extends {@link ThreadGroup}.
 *
 * @see ProcRef
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

  /**
   * The {@link Node} for this Proc.
   */
  private final Node node;

  /**
   * The process id ({@link ProcRef}) of this Proc.
   */
  private final ProcRef selfProcRef;

  /**
   * The queue of incoming messages. Messages can be of any type.
   */
  private final Mailbox<Object> mailbox;

  /**
   * The set of linked Procs. Needs to be thread-safe.
   *
   * @see #link(ProcRef)
   * @see #unlink(ProcRef)
   * @see #completeLink(ProcRef)
   * @see #completeUnlink(ProcRef)
   */
  private final Set<ProcRef> links;

  /**
   * @param node The {@link Node} this Proc is running on.
   */
  public Proc(Node node) {
    super(node, "");

    this.node = requireNonNull(node, "Proc cannot be bound to null Node.");
    ;
    selfProcRef = new LocalProcRef(this);
    mailbox = new Mailbox<>();
    links = Collections.newSetFromMap(new ConcurrentHashMap<>());
    setName(selfProcRef.toString());
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
    requireNonNull(msg);

    if (msg instanceof SystemMail)
    // TODO In the future this will have to handle
    // procs being configured to trap system mail.
    {
      ((SystemMail) msg).visit(this);
    } else if (!mailbox.offer(msg)) {
      assert false : "Unreachable, mailbox has no max capacity.";
    }
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
      log.debug("{} was interrupted.", selfProcRef, e);
      reason = e;
    } catch (Exception e) {
      if (e == NORMAL_EXIT) {
        log.debug("{} exited.", selfProcRef);
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
    ProcRef self = self();
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
   * Get a reference to this Proc.
   */
  public final ProcRef self() {
    return this.selfProcRef;
  }

  /**
   * Get a reference to this Proc's Node.
   */
  public final NodeRef node() {
    return node.self();
  }

  /**
   * Receive a message within the given timeout, otherwise run a timeout handler.
   *
   * @param handler        A consumer for the received message.
   * @param timeout        The duration to wait for a message.
   * @param timeoutHandler The action to take if the receive times out.
   * @throws InterruptedException If the Proc is interrupted while waiting for mail.
   * @see #receive(Lambda.One, Duration)
   * @see #receive(Lambda.One)
   */
  protected final void receive(Lambda.One<Object> handler, Duration timeout, Runnable timeoutHandler)
      throws Exception {
    requireNonNull(handler, "recieve requires a message handler.");
    if (timeout == null && timeoutHandler != null) {
      throw new NullPointerException(
          "receive requires a valid timeout if the timeoutHandler is not null");
    }

    Object mail;
    if (timeout == null) {
      mail = mailbox.take();
    } else if (timeout.toNanos() == 0) {
      mail = mailbox.poll();
    } else {
      mail = mailbox.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    if (mail == null && timeoutHandler != null) {
      timeoutHandler.run();
    } else if (mail != null) {
      handler.accept(mail);
    }
  }

  /**
   * Receive mail within a timeout, taking no action if the timeout expires.
   *
   * @param handler A consumer for the received message.
   * @param timeout The duration to wait for a message.
   * @throws InterruptedException If the Proc is interrupted while waiting for mail.
   * @see #receive(Lambda.One, Duration, Runnable)
   * @see #receive(Lambda.One)
   */
  protected final void receive(Lambda.One<Object> handler, Duration timeout)
      throws Exception {
    receive(handler, timeout, null);
  }

  /**
   * Receive mail, blocking indefinetly.
   *
   * @param handler A consumer for the received message.
   * @throws InterruptedException If the Proc is interrupted while waiting for mail.
   * @see #receive(Lambda.One, Duration, Runnable)
   * @see #receive(Lambda.One, Duration)
   */
  protected final void receive(Lambda.One<Object> handler)
      throws Exception {
    receive(handler, null, null);
  }

  /**
   * Receive a message within the given timeout, otherwise run a timeout handler.
   *
   * @param handler        A consumer for the received message.
   * @param timeout        The duration to wait for a message.
   * @param timeoutHandler The action to take if the receive times out.
   * @throws InterruptedException If the Proc is interrupted while waiting for mail.
   * @see #receive(Lambda.One, Duration)
   * @see #receive(Lambda.One)
   */
  protected final void receive(PartialConsumer handler, Duration timeout, Runnable timeoutHandler)
      throws Exception {
    requireNonNull(handler, "recieve requires a message handler.");
    if (timeout == null && timeoutHandler != null) {
      throw new NullPointerException(
          "receive requires a valid timeout if the timeoutHandler is not null");
    }

    Object mail;
    if (timeout == null) {
      mail = mailbox.takeMatch(handler::isDefinedAt);
    } else if (timeout.toNanos() == 0) {
      mail = mailbox.pollMatch(handler::isDefinedAt);
    } else {
      mail = mailbox.pollMatch(handler::isDefinedAt, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    if (mail == null && timeoutHandler != null) {
      timeoutHandler.run();
    } else if (mail != null) {
      handler.accept(mail);
    }
  }

  /**
   * Receive mail within a timeout, taking no action if the timeout expires.
   *
   * @param handler A consumer for the received message.
   * @param timeout The duration to wait for a message.
   * @throws InterruptedException If the Proc is interrupted while waiting for mail.
   * @see #receive(Lambda.One, Duration, Runnable)
   * @see #receive(Lambda.One)
   */
  protected final void receive(PartialConsumer handler, Duration timeout)
      throws Exception {
    receive(handler, timeout, null);
  }

  /**
   * Receive mail, blocking indefinetly.
   *
   * @param handler A consumer for the received message.
   * @throws InterruptedException If the Proc is interrupted while waiting for mail.
   * @see #receive(Lambda.One, Duration, Runnable)
   * @see #receive(Lambda.One, Duration)
   */
  protected final void receive(PartialConsumer handler)
      throws Exception {
    receive(handler, null, null);
  }

  /**
   * Exit the Proc immediately. This can be used to return from inside of lambdas.
   * Inside of {@link #main()}, it has the same effect as {@code return;}.
   */
  protected final void exit() {
    throw NORMAL_EXIT;
  }

  /**
   * Create a link between this and the target Proc.
   * If a link already exists, this method has no
   * effect.
   *
   * @param other The {@link ProcRef} of the Proc to be linked.
   */
  protected final void link(ProcRef other) {
    completeLink(other);
    other.send(new SystemMail.Link(selfProcRef));
  }

  /**
   * Destroy any link between this process and another.
   * If no link exists, this method has no effect.
   *
   * @param other The {@link ProcRef} of the Proc to be unlinked.
   */
  protected final void unlink(ProcRef other) {
    completeUnlink(other);
    other.send(new SystemMail.Unlink(selfProcRef));
  }

  final void completeLink(ProcRef other) {
    log.debug("Linked {} and {}.", this, other);
    links.add(other);
  }

  final void completeUnlink(ProcRef other) {
    log.debug("Unlinked {} and {}.", this, other);
    links.remove(other);
  }
}

