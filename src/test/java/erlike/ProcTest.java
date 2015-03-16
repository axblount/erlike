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

import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static erlike.Library.*;
import static org.junit.Assert.*;

public class ProcTest {
    private class Flag {
        volatile boolean flag = false;
        void set() { flag = true; }
        boolean isSet() { return flag; }
        void reset() { flag = false; }
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

        node.joinAll();

        assertFalse("success flag was set.", successFlag.isSet());
        assertTrue("timeout flag was not set.", timeoutFlag.isSet());
        assertTrue("continue flag was not set.", continueFlag.isSet());
    }

    @Test
    public void patternReceiveTest() throws InterruptedException {
        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");

        class A {}
        class B {}

        Node node = new Node("patternReceiveTest");
        final Flag aFlag = new Flag();
        final Flag bFlag = new Flag();

        Pid pid = node.spawn(() -> {
            PartialConsumer handler = new PartialConsumer()
                    .match(A.class, a ->
                            aFlag.set())
                    .match(B.class, b ->
                            bFlag.set())
                    .otherwise(obj -> exit());
            while (true) {
                receive(handler);
            }
        });

        Thread.sleep(1000);

        pid.send(new A());
        Thread.sleep(100);
        assertEquals("there were errors.", 0, node.getUncaughtExceptions().size());
        assertTrue("A flag was not set.", aFlag.isSet());
        assertFalse("B flag was set by A.", bFlag.isSet());
        aFlag.reset();

        pid.send(new B());
        Thread.sleep(100);
        assertEquals("there were errors.", 0, node.getUncaughtExceptions().size());
        assertFalse("A flag was set by B.", aFlag.isSet());
        assertTrue("B flag was not set.", bFlag.isSet());
        bFlag.reset();

        pid.send(new Object());
        node.joinAll();
        assertEquals("there were errors.", 0, node.getUncaughtExceptions().size());
        assertFalse("A flag was set by an object.", aFlag.isSet());
        assertFalse("B flag was set by a object.", bFlag.isSet());
    }
}
