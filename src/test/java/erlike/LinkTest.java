package erlike;

import org.junit.*;

import static java.time.Duration.ofMillis;
import static org.junit.Assert.*;

import static erlike.Library.*;

public class LinkTest {
    @Test
    public void linkChainExample() throws InterruptedException {
        final RuntimeException breakTheChain = new RuntimeException("Break the Chain!");

        Node node = new Node("linkChainExample");

        // This is the canary, when it stops printing the chain is dead!
        Pid last = node.spawn(() -> {
            int i = 0;
            for (;;) {
                Thread.sleep(200);
                assertTrue("The canary lived too long.", i++ < 5);
            }
        });

        for (int i = 0; i < 100; i++) {
            last = node.spawn(pid -> {
                link(pid);
                Thread.sleep(2000);
                System.out.println("Link chain should die before this prints!");
            }, last);
        }

        node.spawn(pid -> {
            link(pid);
            Thread.sleep(1000);

            // this exception should cause the link chain to explode
            throw breakTheChain;
        }, last);

        // wait for all procs to finish
        node.joinAll();

        assertTrue(node.getUncaughtExceptions().size() == 1);
        assertTrue(node.getUncaughtExceptions().contains(breakTheChain));
    }

    @Test
    public void linkShouldntBreak() throws InterruptedException {
        Node node = new Node("linkShouldntBreak");

        Pid first = node.spawn(() -> {
            // exit normally as soon as we recieve a message.
            // A normal exit should not cause the second proc to die.
            receive(obj -> exit());
        });

        Pid watcher = node.spawn(() -> {
            receive(obj -> exit(),
                    ofMillis(1000),
                    () -> fail("The link killed the other process"));
        });

        node.spawn(() -> {
            link(first);
            first.send(1);
            Thread.sleep(250);
            watcher.send(1);
        });

        node.joinAll();

        // We should have no exceptions.
        assertEquals(0, node.getUncaughtExceptions().size());
    }
}
