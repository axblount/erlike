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

import static java.time.Duration.ofMillis;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import static erlike.Library.*;

public class LinkTest {
    @Test
    public void linkChainExample() throws InterruptedException {
        final RuntimeException breakTheChain = new RuntimeException("Break the Chain!");

        Node node = new Node("linkChainExample");

        // This is the canary, when it stops printing the chain is dead!
        ProcId last = node.spawn(() -> {
            Thread.sleep(2000);
            fail("The canary lived too long.");
        });

        for (int i = 0; i < 100; i++) {
            last = node.spawn(pid -> {
                System.out.println(String.format("Linked %s to %s", self(), pid));
                link(pid);
                Thread.sleep(2000);
                fail("Link chain did not terminate.");
            }, last);
        }

        node.spawn(pid -> {
            System.out.println(String.format("Linked %s to %s", self(), pid));
            link(pid);
            Thread.sleep(1000);

            // this exception should cause the link chain to explode
            System.out.println("breaking the chain...");
            throw breakTheChain;
        }, last);

        // wait for all procs to finish
        node.joinAll();

        assertThat(node.getUncaughtExceptions().size(), is(1));
        assertThat(node.getUncaughtExceptions(), contains(breakTheChain));
    }

    @Test
    public void linkShouldntBreak() throws InterruptedException {
        Node node = new Node("linkShouldntBreak");

        ProcId first = node.spawn(() -> {
            // exit normally as soon as we receive a message.
            // A normal exit should not cause the second proc to die.
            receive(obj -> exit());
        });

        ProcId watcher = node.spawn(() -> {
            receive(obj -> exit(),
                    ofMillis(1000),
                    () -> fail("The link killed the other process."));
        });

        node.spawn(() -> {
            link(first);
            first.send(1);
            Thread.sleep(250);
            watcher.send(1);
        });

        node.joinAll();

        // We should have no exceptions.
        assertThat(node.getUncaughtExceptions().size(), is(0));
    }
}
