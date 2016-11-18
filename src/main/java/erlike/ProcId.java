package erlike;

/**
 * Created by alex on 11/17/16.
 */
public interface ProcId {
    void send(Object message);
    long id();
    NodeId node();
}
