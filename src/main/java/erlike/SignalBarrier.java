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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

/**
 * A simple barrier for awaiting a signal.
 * Only one thread at a time may await the signal.
 *
 * This class uses non-standard parts of the Java language.
 * Notably {@link LockSupport}, which is provided by OpenJDK
 * and the Oracle JDK, but isn't in the Java standard.
 *
 * @see LockSupport A non-standard class used to control threads.
 * @see LockSupport#park(Object) Used to pause threads that are waiting for a SignalBarrier.
 * @see LockSupport#unpark(Thread) Used to awaken threads when a SignalBarrier is signalled.
 */
class SignalBarrier {
  private static final Logger log = LoggerFactory.getLogger(SignalBarrier.class);

  /**
   * The Thread that is currently awaiting the signal.
   * !!! NEVER call this directly !!!
   */
  @SuppressWarnings("unused")
  private volatile Thread _owner;

  /**
   * Used to update the owner atomically.
   */
  private static final AtomicReferenceFieldUpdater<SignalBarrier, Thread> ownerAccess =
      AtomicReferenceFieldUpdater.newUpdater(SignalBarrier.class, Thread.class, "_owner");

  /**
   * Create an ownerless SignalBarrier.
   */
  public SignalBarrier() {
    _owner = null;
  }

  /**
   * Signal the owner that the barrier is ready.
   * This has no effect if the SignalBarrier is unowned.
   */
  public void signal() {
    // Remove the current owner of this barrier.
    Thread t = ownerAccess.getAndSet(this, null);

    // If the owner wasn't null, unpark it.
    if (t != null) {
      LockSupport.unpark(t);
      log.debug("SignalBarrier signaled. {} to be unparked.", t);
    }
  }

  /**
   * Claim the SignalBarrier and block until signaled.
   *
   * @throws IllegalStateException If the SignalBarrier already has an owner.
   * @throws InterruptedException  If the thread is interrupted while waiting.
   */
  public void await() throws InterruptedException {
    // Get the thread that would like to await the signal.
    Thread t = Thread.currentThread();

    // If a thread is attempting to await, the current owner should be null.
    if (!ownerAccess.compareAndSet(this, null, t)) {
      log.error(
          "{} attempted to use a SignalBarrier already in use by {}", t, ownerAccess.get(this));
      throw new IllegalStateException(
          "A second thread tried to acquire a signal barrier that is already owned.");
    }

    // The current thread has taken ownership of this barrier.
    // Park the current thread until the signal. Record this
    // signal barrier as the 'blocker'.
    log.debug("SignalBarrier created and {} parked.", t);
    LockSupport.park(this);

    // If a thread has called #signal() the owner should already be null.
    // However the documentation for LockSupport#unpark(Thread) makes it clear that
    // threads can wake up for absolutely no reason. Do a compare and set
    // to make sure we don't wipe out a new owner, keeping in mind that only one
    // thread should be awaiting at any given moment!
    ownerAccess.compareAndSet(this, t, null);
    log.debug("SignalBarrier broken and {} unparked.", t);

    // Check to see if we've been unparked because of a thread interrupt.
    if (t.isInterrupted()) {
      throw new InterruptedException();
    }
  }

  /**
   * Claim the SignalBarrier and block until signaled or the timeout expires.
   *
   * @param timeout The timeout duration.
   * @param unit    The units of the timeout duration.
   * @throws IllegalStateException If the SignalBarrier already has an owner.
   * @throws InterruptedException  If the thread is interrupted while waiting.
   * @see #awaitNanos(long)
   */
  public void await(long timeout, TimeUnit unit) throws InterruptedException {
    awaitNanos(unit.toNanos(timeout));
  }

  /**
   * Claim the SignalBarrier and block until signaled or the timeout expires.
   *
   * @param timeout The timeout duration in nanoseconds.
   * @return The timeout minus the number of nanoseconds that passed while waiting.
   * @throws IllegalStateException If the SignalBarrier already has an owner.
   * @throws InterruptedException  If the thread is interrupted while waiting.
   */
  public long awaitNanos(long timeout) throws InterruptedException {
    if (timeout <= 0) {
      return 0;
    }
    // Get the thread that would like to await the signal.
    Thread t = Thread.currentThread();

    // If a thread is attempting to await, the current owner should be null.
    if (!ownerAccess.compareAndSet(this, null, t)) {
      log.error(
          "{} attempted to use a SignalBarrier already in use by {}", t, ownerAccess.get(this));
      throw new IllegalStateException(
          "A second thread tried to acquire a signal barrier that is already owned.");
    }

    // The current thread owns this barrier.
    // Park it until the signal.
    // Time the park.
    long start = System.nanoTime();
    log.debug("SignalBarrier created and {} parked.", t);
    LockSupport.parkNanos(this, timeout);
    ownerAccess.compareAndSet(this, t, null);
    log.debug("SignalBarrier broken and {} unparked.", t);
    long stop = System.nanoTime();

    // Check to see if we've been unparked because of a thread interrupt.
    if (t.isInterrupted()) {
      throw new InterruptedException();
    }

    // Return the number of nanoseconds left in the timeout after what we
    // just waited.
    return Math.max(timeout - stop + start, 0L);
  }
}
