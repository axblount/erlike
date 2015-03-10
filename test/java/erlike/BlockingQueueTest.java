/*
 * Written by Doug Lea and Martin Buchholz with assistance from members
 * of JCP JSR-166 Expert Group and released to the public domain, as
 * explained at http://creativecommons.org/publicdomain/zero/1.0/
 *
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 *
 * Modified for this project.
 */

package erlike;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeoutException;

import org.junit.*;
import static org.junit.Assert.*;

/**
 * Contains "contract" tests applicable to all BlockingQueue implementations.
 */
@Ignore
public abstract class BlockingQueueTest {
    /*
     * This is the start of an attempt to refactor the tests for the
     * various related implementations of related interfaces without
     * too much duplicated code.  junit does not really support such
     * testing.  Here subclasses of TestCase not only contain tests,
     * but also configuration information that describes the
     * implementation class, most importantly how to instantiate
     * instances.
     */

    /** Returns an empty instance of the implementation class. */
    protected abstract BlockingQueue emptyCollection();

    /**
     * Returns an element suitable for insertion in the collection.
     * Override for collections with unusual element types.
     */
    protected Object makeElement(int i) {
        return Integer.valueOf(i);
    }

    private static final long SHORT_DELAY_MS = 50;
    private static final long SMALL_DELAY_MS = SHORT_DELAY_MS * 5;
    private static final long MEDIUM_DELAY_MS = SHORT_DELAY_MS * 10;
    private static final long LONG_DELAY_MS = SHORT_DELAY_MS * 200;

    /**
     * Returns a timeout in milliseconds to be used in tests that
     * verify that operations block or time out.
     */
    long timeoutMillis() {
        return SHORT_DELAY_MS / 4;
    }

    private static final int SIZE = 10;

    protected long millisElapsedSince(long ago) {
        return System.currentTimeMillis() - ago;
    }

    protected void shouldThrow() {
        fail("Exception wasn't thrown");
    }

    /**
     * Returns a new started daemon Thread running the given runnable.
     */
    Thread newStartedThread(Runnable runnable) {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        t.start();
        return t;
    }

    public void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(LONG_DELAY_MS, MILLISECONDS));
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }

    /**
     * Checks that thread does not terminate within the default
     * millisecond delay of {@code timeoutMillis()}.
     */
    void assertThreadStaysAlive(Thread thread) {
        assertThreadStaysAlive(thread, timeoutMillis());
    }

    /**
     * Checks that thread does not terminate within the given millisecond delay.
     */
    void assertThreadStaysAlive(Thread thread, long millis) {
        try {
            // No need to optimize the failing case via Thread.join.
            delay(millis);
            assertTrue(thread.isAlive());
        } catch (InterruptedException fail) {
            fail("Unexpected InterruptedException");
        }
    }

    /**
     * Delays, via Thread.sleep, for the given millisecond delay, but
     * if the sleep is shorter than specified, may re-sleep or yield
     * until time elapses.
     */
    static void delay(long millis) throws InterruptedException {
        long startTime = System.nanoTime();
        long ns = millis * 1000 * 1000;
        for (;;) {
            if (millis > 0L)
                Thread.sleep(millis);
            else // too short to sleep
                Thread.yield();
            long d = ns - (System.nanoTime() - startTime);
            if (d > 0L)
                millis = d / (1000 * 1000);
            else
                break;
        }
    }

    /**
     * Waits for the specified time (in milliseconds) for the thread
     * to terminate (using {@link Thread#join(long)}), else interrupts
     * the thread (in the hope that it may terminate later) and fails.
     */
    void awaitTermination(Thread t, long timeoutMillis) {
        try {
            t.join(timeoutMillis);
        } catch (InterruptedException e) {
            fail("Unexpected interruption: " + e);
        } finally {
            if (t.getState() != Thread.State.TERMINATED) {
                t.interrupt();
                fail("Test timed out");
            }
        }
    }

    //----------------------------------------------------------------
    // Tests
    //----------------------------------------------------------------

    /**
     * offer(null) throws NullPointerException
     */
    @Test(expected=NullPointerException.class)
    public void testOfferNull() {
        final Queue q = emptyCollection();
        q.offer(null);
    }

    /**
     * add(null) throws NullPointerException
     */
    @Test(expected=NullPointerException.class)
    public void testAddNull() {
        final Collection q = emptyCollection();
        q.add(null);
    }

    /**
     * timed offer(null) throws NullPointerException
     */
    @Test
    public void testTimedOfferNull() throws InterruptedException {
        final BlockingQueue q = emptyCollection();
        long startTime = System.nanoTime();
        try {
            q.offer(null, LONG_DELAY_MS, MILLISECONDS);
            fail("Should throw NPE");
        } catch (NullPointerException success) {}
        assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
    }

    /**
     * put(null) throws NullPointerException
     */
    @Test(expected=NullPointerException.class)
    public void testPutNull() throws InterruptedException {
        final BlockingQueue q = emptyCollection();
        q.put(null);
    }

    /**
     * put(null) throws NullPointerException
     */
    @Test(expected=NullPointerException.class)
    public void testAddAllNull() throws InterruptedException {
        final Collection q = emptyCollection();
        q.addAll(null);
    }

    /**
     * addAll of a collection with null elements throws NullPointerException
     */
    @Test(expected=NullPointerException.class)
    public void testAddAllNullElements() {
        final Collection q = emptyCollection();
        final Collection<Integer> elements = Arrays.asList(new Integer[SIZE]);
        q.addAll(elements);
    }

    /**
     * toArray(null) throws NullPointerException
     */
    @Test(expected=NullPointerException.class)
    public void testToArray_NullArray() {
        final Collection q = emptyCollection();
        q.toArray(null);
    }

    /**
     * drainTo(null) throws NullPointerException
     */
    @Test(expected=NullPointerException.class)
    public void testDrainToNull() {
        final BlockingQueue q = emptyCollection();
        q.drainTo(null);
    }

    /**
     * drainTo(this) throws IllegalArgumentException
     */
    @Test(expected=IllegalArgumentException.class)
    public void testDrainToSelf() {
        final BlockingQueue q = emptyCollection();
        q.drainTo(q);
    }

    /**
     * drainTo(null, n) throws NullPointerException
     */
    @Test(expected=NullPointerException.class)
    public void testDrainToNullN() {
        final BlockingQueue q = emptyCollection();
        q.drainTo(null, 0);
    }

    /**
     * drainTo(this, n) throws IllegalArgumentException
     */
    @Test(expected=IllegalArgumentException.class)
    public void testDrainToSelfN() {
        final BlockingQueue q = emptyCollection();
        q.drainTo(q, 0);
    }

    /**
     * drainTo(c, n) returns 0 and does nothing when n <= 0
     */
    @Test
    public void testDrainToNonPositiveMaxElements() {
        final BlockingQueue q = emptyCollection();
        final int[] ns = { 0, -1, -42, Integer.MIN_VALUE };
        for (int n : ns)
            assertEquals(0, q.drainTo(new ArrayList(), n));
        if (q.remainingCapacity() > 0) {
            // Not SynchronousQueue, that is
            Object one = makeElement(1);
            q.add(one);
            ArrayList c = new ArrayList();
            for (int n : ns)
                assertEquals(0, q.drainTo(new ArrayList(), n));
            assertEquals(1, q.size());
            assertSame(one, q.poll());
            assertTrue(c.isEmpty());
        }
    }

    /**
     * timed poll before a delayed offer times out; after offer succeeds;
     * on interruption throws
     */
    @Test
    public void testTimedPollWithOffer() throws InterruptedException {
        final BlockingQueue q = emptyCollection();
        final CheckedBarrier barrier = new CheckedBarrier(2);
        final Object zero = makeElement(0);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                long startTime = System.nanoTime();
                assertNull(q.poll(timeoutMillis(), MILLISECONDS));
                assertTrue(millisElapsedSince(startTime) >= timeoutMillis());

                barrier.await();

                assertSame(zero, q.poll(LONG_DELAY_MS, MILLISECONDS));

                Thread.currentThread().interrupt();
                try {
                    q.poll(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                barrier.await();
                try {
                    q.poll(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        barrier.await();
        long startTime = System.nanoTime();
        assertTrue(q.offer(zero, LONG_DELAY_MS, MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);

        barrier.await();
        assertThreadStaysAlive(t);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * take() blocks interruptibly when empty
     */
    @Test
    public void testTakeFromEmptyBlocksInterruptibly() {
        final BlockingQueue q = emptyCollection();
        final CountDownLatch threadStarted = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                threadStarted.countDown();
                try {
                    q.take();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(threadStarted);
        assertThreadStaysAlive(t);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * take() throws InterruptedException immediately if interrupted
     * before waiting
     */
    @Test
    public void testTakeFromEmptyAfterInterrupt() {
        final BlockingQueue q = emptyCollection();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                Thread.currentThread().interrupt();
                try {
                    q.take();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        awaitTermination(t);
    }

    /**
     * timed poll() blocks interruptibly when empty
     */
    @Test
    public void testTimedPollFromEmptyBlocksInterruptibly() {
        final BlockingQueue q = emptyCollection();
        final CountDownLatch threadStarted = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                threadStarted.countDown();
                try {
                    q.poll(2 * LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(threadStarted);
        assertThreadStaysAlive(t);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * timed poll() throws InterruptedException immediately if
     * interrupted before waiting
     */
    @Test
    public void testTimedPollFromEmptyAfterInterrupt() {
        final BlockingQueue q = emptyCollection();
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() {
                Thread.currentThread().interrupt();
                try {
                    q.poll(2 * LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        awaitTermination(t);
    }

    /**
     * remove(x) removes x and returns true if present
     * TODO: move to superclass CollectionTest.java
     */
    @Test
    public void testRemoveElement() {
        final BlockingQueue q = emptyCollection();
        final int size = Math.min(q.remainingCapacity(), SIZE);
        final Object[] elts = new Object[size];
        assertFalse(q.contains(makeElement(99)));
        assertFalse(q.remove(makeElement(99)));
        checkEmpty(q);
        for (int i = 0; i < size; i++)
            q.add(elts[i] = makeElement(i));
        for (int i = 1; i < size; i += 2) {
            for (int pass = 0; pass < 2; pass++) {
                assertEquals((pass == 0), q.contains(elts[i]));
                assertEquals((pass == 0), q.remove(elts[i]));
                assertFalse(q.contains(elts[i]));
                assertTrue(q.contains(elts[i-1]));
                if (i < size - 1)
                    assertTrue(q.contains(elts[i+1]));
            }
        }
        if (size > 0)
            assertTrue(q.contains(elts[0]));
        for (int i = size-2; i >= 0; i -= 2) {
            assertTrue(q.contains(elts[i]));
            assertFalse(q.contains(elts[i+1]));
            assertTrue(q.remove(elts[i]));
            assertFalse(q.contains(elts[i]));
            assertFalse(q.remove(elts[i+1]));
            assertFalse(q.contains(elts[i+1]));
        }
        checkEmpty(q);
    }

    /**
     * A CyclicBarrier that uses timed await and fails with
     * AssertionFailedErrors instead of throwing checked exceptions.
     */
    public class CheckedBarrier extends CyclicBarrier {
        public CheckedBarrier(int parties) { super(parties); }

        public int await() {
            try {
                return super.await(2 * LONG_DELAY_MS, MILLISECONDS);
            } catch (TimeoutException timedOut) {
                fail("CheckedBarrier#await() timed out");
            } catch (Exception e) {
                fail("Unexpected exception " + e);
            }
            throw new RuntimeException("Unexpected exception");
        }
    }

    void checkEmpty(BlockingQueue q) {
        try {
            assertTrue(q.isEmpty());
            assertEquals(0, q.size());
            assertNull(q.peek());
            assertNull(q.poll());
            assertNull(q.poll(0, MILLISECONDS));
            assertEquals(q.toString(), "[]");
            assertTrue(Arrays.equals(q.toArray(), new Object[0]));
            assertFalse(q.iterator().hasNext());
            try {
                q.element();
                fail("#element() should throw when queue is empty");
            } catch (NoSuchElementException success) {}
            try {
                q.iterator().next();
                fail("#iterator().next() should throw when queue is empty");
            } catch (NoSuchElementException success) {}
            try {
                q.remove();
                shouldThrow();
                fail("#remove() should throw when queue is empty");
            }
            catch (NoSuchElementException success) {}
        } catch (InterruptedException e) { fail("Unexpected interruption: " + e); }
    }

    /**
     * Waits for LONG_DELAY_MS milliseconds for the thread to
     * terminate (using {@link Thread#join(long)}), else interrupts
     * the thread (in the hope that it may terminate later) and fails.
     */
    void awaitTermination(Thread t) {
        awaitTermination(t, LONG_DELAY_MS);
    }

    // Some convenient Runnable classes

    public abstract class CheckedRunnable implements Runnable {
        protected abstract void realRun() throws Exception;

        public final void run() {
            try {
                realRun();
            } catch (Exception e) {
                fail("Unexpected exception: " + e + "\n" + e.getMessage());
            }
        }
    }
}
