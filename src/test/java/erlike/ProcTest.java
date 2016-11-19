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
import static org.hamcrest.Matchers.*;

import java.time.Duration;

import erlike.functions.PartialConsumer;

import static erlike.Library.*;
import static org.junit.Assert.*;

public class ProcTest {
    private class Flag {
        volatile boolean flag = false;
        void raise() { flag = true; }
        boolean isRaised() { return flag; }
        void reset() { flag = false; }
    }

    @Test
    public void procAsThreadTest() throws InterruptedException {
        Node node = new Node("procStart");

        ProcRef pid = node.spawn(() -> Thread.sleep(5000));
        Proc proc = node.unsafeGetProc(pid);

        Thread.sleep(100);
        assertThat("Thread didn't sleep.", proc.getState(), is(Thread.State.TIMED_WAITING));

        proc.interrupt();
        Thread.sleep(100);
        assertThat("Thread didn't terminate.", proc.getState(), is(Thread.State.TERMINATED));
    }

    @Test
    public void basicReceiveTest() throws InterruptedException {
        Node node = new Node("basicReceiveTest");
        final Flag flag = new Flag();

        ProcRef pid = node.spawn(() -> {
            receive(obj -> flag.raise());
        });

        pid.send(1);
        Thread.sleep(100);
        assertThat("The proc didn't receive the message.",
                flag.isRaised(), is(true));
    }

    @Test
    public void receiveTimeoutTest() throws InterruptedException {
        Node node = new Node("receiveTimeoutTest");
        final Flag timeoutFlag = new Flag();
        final Flag successFlag = new Flag();
        final Flag continueFlag = new Flag();

        ProcRef pid = node.spawn(() -> {
            receive(obj -> {
                        System.err.println(obj);
                        successFlag.raise();
                    },
                    Duration.ofMillis(100),
                    () -> timeoutFlag.raise());
            continueFlag.raise();
        });

        node.joinAll();

        assertThat("The success flag was raise. It should not have been reached.",
                successFlag.isRaised(), is(false));
        assertThat("The timeout flag was not raise. The timeout handler was never called.",
                timeoutFlag.isRaised(), is(true));
        assertThat("The continue flag was not raise. The proc did not continue after the receive timed out.",
                continueFlag.isRaised(), is(true));
    }

    @Test
    public void patternReceiveTest() throws InterruptedException {
        // System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");

        class A {}
        class B {}

        Node node = new Node("patternReceiveTest");
        final Flag aFlag = new Flag();
        final Flag bFlag = new Flag();

        ProcRef pid = node.spawn(() -> {
            while (true) {
                receive(new PartialConsumer()
                        .match(A.class, a -> aFlag.raise())
                        .match(B.class, b -> bFlag.raise())
                        .otherwise(obj -> exit()));
            }
        });

        Thread.sleep(1000);

        pid.send(new A());
        Thread.sleep(100);
        assertThat("A proc raised an unexpected exception after an 'A' message was sent.",
                node.getUncaughtExceptions(), empty());
        assertThat("'A' flag was not raised when an 'A' message was sent.",
                aFlag.isRaised(), is(true));
        assertThat("'A' flag was raised when a 'B' message was sent.",
                bFlag.isRaised(), is(false));
        aFlag.reset();

        pid.send(new B());
        Thread.sleep(100);
        assertThat("A proc raised an unexpected exception after a 'B' message was sent.",
                node.getUncaughtExceptions(), empty());
        assertThat("'A' flag was raised when a 'B' message was sent.",
                aFlag.isRaised(), is(false));
        assertThat("'B' flag was not raised when an 'A' message was sent.",
                bFlag.isRaised(), is(true));
        bFlag.reset();

        pid.send(new Object());
        node.joinAll();
        assertThat("A proc raised an unexpected exception after the exit message was sent.",
                node.getUncaughtExceptions(), empty());
        assertThat("'A' flag was raised by a plain old object message.",
                aFlag.isRaised(), is(false));
        assertThat("'B' flag was raised by a plain old object message.",
                bFlag.isRaised(), is(false));
    }
}
