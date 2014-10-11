package erlike;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** TODO: don't extend Thread, too many method collision possibilities. */
public abstract class Proc extends Thread {
    private static Exception QUIT = new RuntimeException() {
        public static final long serialVersionUID = 0xDEAD;
    };
    private final Node node;
    private final int id;
    private final BlockingQueue<Object> mailbox;

    protected Proc(final Node node, final int id) {
        // using node as a ThreadGroup
        super(node, "proc_name"); // TODO
        this.node = node;
        this.id = id;
        this.mailbox = new LinkedBlockingQueue<Object>();
    }

    protected abstract void main() throws Exception;

    public Pid self() {
        return new PidImpl(node, Node.SELF_NODE_ID, id);
    }

    /*package-local*/ final void addMail(Object msg) {
        mailbox.offer(msg);
    }

    @Override public final void run() {
        try {
            main();
        } catch (Exception e) {
            // TODO: provide e to the Node as a crash reason
            if (e != QUIT)
                e.printStackTrace();
        }
        node.notifyQuit(this);
    }

    /*=====================*/
    /*  BIFs               */
    /*=====================*/

    protected final void receive(Duration timeout, Consumer<Object> handler, Runnable timeoutHandler)
    throws InterruptedException {
        if (timeout == null || handler == null)
            throw new RuntimeException("receive requires a valid timeout and handler");
        Object msg = mailbox.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (msg == null && timeoutHandler != null)
            timeoutHandler.run();
        handler.accept(msg);
    }

    protected final void receive(Duration timeout, Consumer<Object> handler)
    throws InterruptedException {
        receive(timeout, handler, null);
    }

    protected final void receive(Consumer<Object> handler)
    throws InterruptedException {
        handler.accept(mailbox.take());
    }

    public final Pid spawn(Class<? extends Proc> procType) {
        return node.spawn(procType);
    }

    public final void checkForInterrupt() throws Exception {
        if (this.isInterrupted())
            throw QUIT;
    }
}

