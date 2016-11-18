package erlike;

/**
 * All references to {@link Proc}s are handled transparently through this class.
 * It does not matter if the proc is running on the same node or in a different
 * hemisphere.
 * <p>
 * All you need to be able to do is send the Proc messages and get the
 * Proc's {@link Node}.
 */
public interface ProcId {
    void send(Object message);
    // FIXME: this should not be here.
    // Instead I'll have to add instanceof checks to distinguish between local/remote ids.
    long id();
    NodeId node();
}
