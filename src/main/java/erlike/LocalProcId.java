package erlike;

/**
 * This is a {@link ProcId} that represents a {@link Proc} running
 * on this JVM.
 */
class LocalProcId implements ProcId {
    private final Proc proc;

    public LocalProcId(Proc proc) {
        this.proc = proc;
    }

    @Override
    public long id() {
        return proc.getId();
    }

    @Override
    public void send(Object message) {
        proc.addMail(message);
    }

    @Override
    public NodeId node() {
        return proc.node();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof LocalProcId)
            return this.proc.equals(((LocalProcId)other).proc);
        return false;
    }

    @Override
    public int hashCode() {
        return proc.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s->%s", node(), proc.getId());
    }
}
