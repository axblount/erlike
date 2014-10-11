package erlike;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Node extends ThreadGroup {
    public static final int SELF_NODE_ID = 0;

    private final AtomicInteger nextProcId;
    private final AtomicInteger nextNodeId;
    private final String name;
    private final ConcurrentMap<Proc, Integer> procToId;
    private final ConcurrentMap<Integer, Proc> idToProc;

    public Node(String name) {
        super(name);
        this.nextProcId = new AtomicInteger(1000);
        this.nextNodeId = new AtomicInteger(SELF_NODE_ID + 1);
        this.name = name;
        this.procToId = new ConcurrentHashMap<>();
        this.idToProc = new ConcurrentHashMap<>();
    }

    public void joinAll() throws InterruptedException {
        for (Proc p : procToId.keySet())
            p.join();
    }

    /*package-local*/ final void sendById(final int procId, Object msg) {
        Proc proc = idToProc.get(procId);
        if (proc != null)
            proc.addMail(msg);
    }

    /*package-local*/ final void sendById(final int nodeId, final int procId, Object msg) {
        if (nodeId == SELF_NODE_ID)
            sendById(procId, msg);
        // TODO
    }

    private synchronized void addProc(int id, Proc proc) {
        procToId.put(proc, id);
        idToProc.put(id, proc);
    }

    private synchronized void removeProc(Proc proc) {
        Integer id = procToId.remove(proc);
        if (id == null) // proc doesn't exist
            return;
        idToProc.remove(id);
    }

    public Pid spawn(Class<? extends Proc> procType) {
        int procId = nextProcId.getAndIncrement();
        Proc proc;
        try {
            Constructor<? extends Proc> ctor = procType.getConstructor(Node.class, int.class);
            proc = ctor.newInstance(this, procId);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                "Your Proc must provide a constructor that accepts a node and an id");
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Proc constructor threw an error.", e.getCause());
        } catch (Exception e) {
            throw new RuntimeException("Couldn't spawn proc", e);
        }
       
        addProc(procId, proc);
        proc.start();
        return new PidImpl(this, SELF_NODE_ID, procId);
    }

    /*package-local*/ void notifyQuit(Proc proc) {
        //TODO: notify links
        removeProc(proc);
    }

    @Override public void uncaughtException(Thread t, Throwable e) {
        if (t instanceof Proc) { // it better be
            Proc p = (Proc) t;
            notifyQuit(p);
        }
    }
}
