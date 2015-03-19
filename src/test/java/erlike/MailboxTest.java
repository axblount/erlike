package erlike;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;

public class MailboxTest extends BlockingQueueTester {
    @Override
    public BlockingQueue emptyCollection() {
        return new Mailbox<Object>();
    }

    @Override @Test @Ignore("N/A") public void testTimedPollWithOffer() { }
    @Override @Test @Ignore("N/A") public void testRemoveElement() { }

    @Test
    public void singleThreadPoll() throws InterruptedException {
        Mailbox<Integer> mbox = new Mailbox<Integer>();
        mbox.offer(1);
        mbox.offer(2);
        mbox.offer(3);

        assertEquals((int)mbox.poll(), 1);
        assertEquals((int)mbox.poll(), 2);
        assertEquals((int)mbox.poll(), 3);
    }

    @Test
    public void singleTheadPollMatch() throws InterruptedException {
        Mailbox<Integer> mbox = new Mailbox<Integer>();
        mbox.offer(10);
        mbox.offer(1);
        mbox.offer(2);
        mbox.offer(3);
        mbox.offer(4);

        assertEquals(10, (int)mbox.pollMatch(i -> ((int)i) > 2));
        assertEquals(3, (int)mbox.pollMatch(i -> ((int)i) > 2));
        assertEquals(4, (int)mbox.pollMatch(i -> ((int)i) > 2));
        assertEquals(1, (int)mbox.poll());
        assertEquals(2, (int)mbox.poll());
    }

    @Test
    public void pollMatchWithTimeout() throws InterruptedException, ExecutionException {
        ExecutorService ex = Executors.newCachedThreadPool();
        Mailbox<Object> mbox = new Mailbox<Object>();

        // force pollMatch to wait
        Future<Object> f = ex.submit(() -> mbox.pollMatch(x -> x instanceof Integer, 2, TimeUnit.SECONDS));

        Thread.sleep(500);

        // send objects that aren't matches.
        mbox.offer(new Object());
        mbox.offer(new Object());
        mbox.offer("still not it");

        mbox.offer(1);

        assertEquals(1, f.get());
    }

    @Test
    public void remove() throws InterruptedException, ExecutionException {
        Mailbox<Object> mbox = new Mailbox<Object>();

        mbox.offer(1);
        mbox.remove();
        assertEquals(null, mbox.poll());

        mbox.offer(1);
        mbox.offer(2);
        mbox.remove();
        assertEquals(2, (int)mbox.poll());
    }

    @Test
    public void allPollTimeout() throws InterruptedException, ExecutionException {
        ExecutorService ex = Executors.newCachedThreadPool();
        Mailbox<Object> mbox = new Mailbox<Object>();

        // force pollMatch to wait
        Future<Object> f = ex.submit(() -> mbox.pollMatch(x -> x instanceof Integer, 100, TimeUnit.MILLISECONDS));

        // wait past the timeout
        Thread.sleep(200);
        mbox.offer(1);
        assertEquals(null, f.get());

        mbox.clear();

        f = ex.submit(() -> mbox.poll(100, TimeUnit.MILLISECONDS));

        // wait past the timeout
        Thread.sleep(200);
        mbox.offer(1);
        assertEquals(null, f.get());
        assertEquals(1, mbox.poll());
    }
}
