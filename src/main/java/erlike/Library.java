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
import java.util.Random;
import java.util.function.Consumer;

/**
 * This class is statically imported to access builtin {@link Proc} functions
 * ({@link Proc#node()}, {@link Proc#self()}, etc.) in anonymous Procs.
 * See the library methods in Proc for their usage.
 * <p>
 * Calling any of these methods outside of a running Proc will cause
 * a {@link java.lang.IllegalStateException}.
 */
public final class Library {
    private static final Logger log = LoggerFactory.getLogger(Library.class);

    // static class
    private Library() {}

    /**
     * Get the Proc executing this method.
     *
     * @throws java.lang.IllegalStateException If the current thread is not a Proc.
     *
     * @return The current thread as a Proc.
     */
    private static Proc currentProc() {
        final Thread t = Thread.currentThread();
        if (t instanceof Proc) {
            return (Proc)t;
        } else {
            log.error("Non-Proc thread {} attempted to use Erlike Library.", t);
            throw new IllegalStateException("Cannot call Proc Library functions from outside a Proc.");
        }
    }

    /**
     * @see Proc#self()
     * @return The process id of the current Proc.
     */
    public static ProcId self() {
        return currentProc().self();
    }

    /**
     * @see Proc#node()
     * @return The node the current proc is running on.
     */
    public static NodeId node() {
        return currentProc().node();
    }

    /**
     * @see Proc#receive(Lambda.One, Duration, Runnable)
     */
    public static void receive(Lambda.One<Object> handler, Duration timeout, Runnable timeoutHandler)
            throws Exception {
        currentProc().receive(handler, timeout, timeoutHandler);
    }

    /**
     * @see Proc#receive(Lambda.One, Duration)
     */
    public static void receive(Lambda.One<Object> handler, Duration timeout)
            throws Exception {
        receive(handler, timeout, null);
    }

    /**
     * @see Proc#receive(Lambda.One)
     */
    public static void receive(Lambda.One<Object> handler)
            throws Exception {
        receive(handler, null, null);
    }

    /**
     * @see Proc#receive(PartialConsumer, Duration, Runnable)
     */
    public static void receive(PartialConsumer handler, Duration timeout, Runnable timeoutHandler)
            throws Exception {
        currentProc().receive(handler, timeout, timeoutHandler);
    }

    /**
     * @see Proc#receive(PartialConsumer, Duration)
     */
    public static void receive(PartialConsumer handler, Duration timeout)
            throws Exception {
        receive(handler, timeout, null);
    }

    /**
     * @see Proc#receive(PartialConsumer)
     */
    public static void receive(PartialConsumer handler)
            throws Exception {
        receive(handler, null, null);
    }

    /**
     * @see Proc#exit()
     */
    public static void exit() {
        currentProc().exit();
    }

    /**
     * @see Proc#link(ProcId)
     */
    public static void link(ProcId other) {
        currentProc().link(other);
    }

    /**
     * @see Proc#unlink(ProcId)
     */
    public static void unlink(ProcId other) {
        currentProc().unlink(other);
    }
}
