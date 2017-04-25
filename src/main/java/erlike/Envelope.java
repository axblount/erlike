package erlike;

import java.io.Serializable;

/**
 * Created by alex on 11/21/16.
 */
class Envelope implements Serializable {
  private final ProcRef procRef;
  private final Object message;

  Envelope(ProcRef procRef, Object message) {
    this.procRef = procRef;
    this.message = message;
  }

  public void send(Node node) {
    node.send(procRef, message);
  }
}
