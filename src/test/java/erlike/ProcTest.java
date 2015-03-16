package erlike;

import org.junit.*;

import java.time.Duration;

import static erlike.Library.*;
import static org.junit.Assert.*;

public class ProcTest {
    private class Flag {
        volatile boolean flag = false;
        void set() { flag = true; }
        boolean isSet() { return flag; }
    }

    @Test
    public void procAsThreadTest() throws InterruptedException {
        Node node = new Node("procStart");

        Pid pid = node.spawn(() -> Thread.sleep(5000));
        Proc proc = node.unsafeGetProc(pid);

        Thread.sleep(100);
        assertEquals("Thread didn't sleep.", Thread.State.TIMED_WAITING, proc.getState());

        proc.interrupt();
        Thread.sleep(100);
        assertEquals("Thread didn't terminate.", Thread.State.TERMINATED, proc.getState());
    }

    @Test
    public void basicReceiveTest() throws InterruptedException {
        Node node = new Node("basicReceiveTest");
        final Flag flag = new Flag();

        Pid pid = node.spawn(() -> {
            receive(obj -> flag.set());
        });

        pid.send(1);
        Thread.sleep(100);
        assertTrue("flag wasn't set by receive.", flag.isSet());
    }

    @Test
    public void receiveTimeoutTest() throws InterruptedException {
        Node node = new Node("receiveTimeoutTest");
        final Flag timeoutFlag = new Flag();
        final Flag successFlag = new Flag();
        final Flag continueFlag = new Flag();

        Pid pid = node.spawn(() -> {
            receive(obj -> {
                        System.err.println(obj);
                        successFlag.set();
                    },
                    Duration.ofMillis(100),
                    () -> timeoutFlag.set());
            continueFlag.set();
        });

        Thread.sleep(200);

        assertFalse("success flag was set.", successFlag.isSet());
        assertTrue("timeout flag was not set.", timeoutFlag.isSet());
        assertTrue("continue flag was not set.", continueFlag.isSet());
    }
}
