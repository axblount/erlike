package erlike;

import org.slf4j.*;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * This class can be statically imported to access builtin proc functions
 * in anonymous procs. See the protected methods in Proc for their usage.
 *
 * {@code import erlike.BIFs.*;}
 */
public final class BIFs {
    private static final Logger log = LoggerFactory.getLogger(BIFs.class);

    private BIFs() {}

    private static Proc currentProc() {
        Thread t = Thread.currentThread();
        try {
            return (Proc)t;
        } catch (ClassCastException e) {
            log.error("Non-Proc thread {} attempted to use Proc BIFs.", t, e);
            throw new IllegalThreadStateException("Cannot call Proc BIFs from outside a Proc");
        }
    }

    public static void receive(Consumer<Object> handler, Duration timeout, Runnable timeoutHandler) throws InterruptedException {
        currentProc().receive(handler, timeout, timeoutHandler);
    }

    public static void receive(Consumer<Object> handler, Duration timeout) throws InterruptedException {
        receive(handler, timeout, null);
    }

    public static void receive(Consumer<Object> handler) throws InterruptedException {
        receive(handler, null, null);
    }

    public static Pid spawn(Class<? extends Proc> procType) {
        return currentProc().spawn(procType);
    }

    public static Pid spawn(Class<? extends Proc> procType, Object... args) {
        return currentProc().spawn(procType, args);
    }

    public static void checkForInterrupt() throws InterruptedException {
        currentProc().checkForInterrupt();
    }

    public static void exit() {
        currentProc().exit();
    }
}
