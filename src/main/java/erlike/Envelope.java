package erlike;

import java.io.Serializable;

class Envelope implements Serializable {
    public final Pid pid;
    public final Object message;

    public Envelope(Pid pid, Object message) {
        this.pid = pid;
        this.message = message;
    }
}
