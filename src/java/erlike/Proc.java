package erlike;

import java.time.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

/**
 * An Erlike process. This is a specialized thread with builtin functions
 * (i.e. protected final methods). User defined Procs will subclass this.
 */
public abstract class Proc extends Thread {
    private static final Logger log = Logger.getLogger(Proc.class.getName());

    /** A unique exception used to signal that a Proc is exiting. */
    private static final RuntimeException EXIT = new RuntimeException() {
        static final long serialVersionUID = 1L;
    };

    /** Has this Proc been bound to a {@link Node}? */
    private boolean isBound = false;

    /** The {@link Node} this Proc is running on. */
    private Node node;

    /** The process id of this Proc. */
    private Pid pid;

    /** The queue of incoming messages. */
    private BlockingQueue<Object> mailbox;

    /**
     * Bind this Proc to a {@link Node}, establishing its identity.
     * This should be called exactly once.
     *
     * @param node The {@link Node} this Proc is running on.
     * @param pid The {@link Pid} used to identify this node.
     */
    /*package-local*/
    void bindAndStart(final Node node, final Pid pid) {
        if (isBound)
            throw new RuntimeException("Proc cannot be bound twice.");
        this.setName(pid.toString());
        this.setUncaughtExceptionHandler(node);
        this.node = node;
        this.pid = pid;
        this.mailbox = new LinkedBlockingQueue<Object>();
        this.isBound = true;
        this.start();
    }

    /**
     * User defined Procs provide behavior by overriding this method.
     *
     * @throws Exception Procs are allowed to throw exceptions.
     */
    protected abstract void main() throws Exception;

    /**
     * @return The {@link Pid} of this Proc.
     */
    public Pid self() {
        return this.pid;
    }

    /**
     * Add mail to the mailbox.
     */
    /*package-local*/
    final void addMail(Object msg) {
        mailbox.offer(msg);
    }

    /**
     * Run this Proc. Thread related exceptions are handled.
     * The parent {@link Node} is notified when the proc quits.
     */
    @Override
    public final void run() {
        try {
            main();
        } catch (InterruptedException e) {
            // we were 'asked' to shutdown
            log.fine(getName() + " was interrupted.");
        } catch (Exception e) {
            if (e != EXIT)
                log.log(Level.WARNING, getName() + " threw and exception.", e);
        } finally {
            node.notifyExit(this);
        }
    }

    /**
     * Check a piece of mail for any system action that needs to be taken.
     *
     * @param mail The mail to be checked.
     */
    private void checkMail(Object mail) {
        if (mail instanceof SystemMail)
            ((SystemMail)mail).visit(this);
    }

    /*=====================*/
    /*  BIFs               */
    /*=====================*/

    /**
     * Receive a message within the given timeout, otherwise run a timeout handler.
     *
     * @see #receive(Consumer, Duration)
     * @see #receive(Consumer)
     *
     * @throws InterruptedException If the Proc is interrupted while waiting for mail.
     *
     * @param handler A consumer for the received message.
     * @param timeout The duration to wait for a message.
     * @param timeoutHandler The action to take if the receive times out.
     */
    protected final void receive(Consumer<Object> handler, Duration timeout, Runnable timeoutHandler)
      throws InterruptedException {
        if (timeout == null && timeoutHandler != null)
            throw new RuntimeException("receive requires a valid timeout");

        Object mail = mailbox.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);

        if (mail == null && timeoutHandler != null)
            timeoutHandler.run();

        checkMail(mail);
        handler.accept(mail);
    }

    /**
     * Receive mail within a timeout, taking no action if the timeout expires.
     *
     * @see #receive(Consumer, Duration, Runnable)
     * @see #receive(Consumer)
     *
     * @throws InterruptedException If the Proc is interrupted while waiting for mail.
     *
     * @param handler A consumer for the received message.
     * @param timeout The duration to wait for a message.
     */
    protected final void receive(Consumer<Object> handler, Duration timeout)
      throws InterruptedException {
        receive(handler, timeout, null);
    }

    /**
     * Receive mail, blocking indefinetly.
     *
     * @see #receive(Consumer, Duration, Runnable)
     * @see #receive(Consumer, Duration)
     *
     * @throws InterruptedException If the Proc is interrupted while waiting for mail.
     *
     * @param handler A consumer for the received message.
     */
    protected final void receive(Consumer<Object> handler)
      throws InterruptedException {
        if (handler == null)
            throw new RuntimeException("receive requires a valid message handler");
        Object mail = mailbox.take();
        checkMail(mail);
        handler.accept(mail);
    }

    /**
     * Spawn a Proc on the current {@link Node}.
     *
     * @param procType The type of Proc to spawn.
     * @return The Pid of the new Proc.
     */
    protected final Pid spawn(Class<? extends Proc> procType) {
        return node.spawn(procType);
    }

    /**
     * Spawn a Proc with arguments on the current {@link Node}.
     *
     * @param procType The type of Proc to spawn.
     * @param args The arguments for the new Proc.
     * @return The Pid of the new Proc.
     */
    protected final Pid spawn(Class<? extends Proc> procType, Object... args) {
        return node.spawn(procType, args);
    }

    /**
     * Exit if the current thread is interrupted. This method should be run
     * at regular intervals if a Proc is performing some long running task while
     * not communicating with other nodes.
     *
     * @throws InterruptedException If the current Proc has been interrupted.
     */
    protected final void checkForInterrupt() throws InterruptedException {
        if (isInterrupted())
            throw new InterruptedException();
    }

    /**
     * Exit the Proc immediately. This can be used to return from inside of lambdas.
     * Inside main, it has the same effect as {@code return}.
     *
     * @throws Exception The exception signaling a Proc exit.
     */
    protected final void exit() throws Exception {
        throw EXIT;
    }
}

