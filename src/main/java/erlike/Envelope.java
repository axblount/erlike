package erlike;

import java.io.Serializable;

class Envelope implements Serializable {
    public final long procId;
    public final Object message;

    public Envelope(long procId, Object message) {
        this.procId = procId;
        this.message = message;
    }
}
