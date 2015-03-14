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

import org.slf4j.*;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * This class can be statically imported to access builtin proc functions
 * in anonymous procs. See the protected methods in Proc for their usage.
 *
 * {@code import static erlike.Library.*;}
 */
public final class Library {
    private static final Logger log = LoggerFactory.getLogger(Library.class);

    private Library() {}

    private static Proc currentProc() {
        Thread t = Thread.currentThread();
        try {
            return (Proc)t;
        } catch (ClassCastException e) {
            log.error("Non-Proc thread {} attempted to use Proc Library.", t, e);
            throw new IllegalThreadStateException("Cannot call Proc Library functions from outside a Proc");
        }
    }

    public static Pid self() {
        return currentProc().self();
    }

    public static Node node() { return currentProc().node(); }

    public static void receive(Consumer<Object> handler, Duration timeout, Runnable timeoutHandler) throws InterruptedException {
        currentProc().receive(handler, timeout, timeoutHandler);
    }

    public static void receive(Consumer<Object> handler, Duration timeout) throws InterruptedException {
        receive(handler, timeout, null);
    }

    public static void receive(Consumer<Object> handler) throws InterruptedException {
        receive(handler, null, null);
    }

    public static void exit() {
        currentProc().exit();
    }
}
