package erlike;

/**
 * This interface represents the id of a process
 * somewhere within the given Erlike system.
 * The process may be local or remote.
 */
public interface Pid {
    /**
     * Send a message to the process this points to.
     * @param msg The message to send.
     */
    public void send(Object msg);

    /**
     * Get a string representing the process this Pid represents.
     * Two Pids for the same process may have two different names
     * depending on where they are printed.
     *
     * @return A String representing this process.
     */
    public String getName();
}

