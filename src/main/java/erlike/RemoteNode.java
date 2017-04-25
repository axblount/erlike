package erlike;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by alex on 11/21/16.
 */
class RemoteNode {
  private static final Logger log = LoggerFactory.getLogger(RemoteNode.class);
  private final Node parent;
  private final long id;
  private final Socket socket;

  RemoteNode(Node parent, long id, Socket socket) {
    this.parent = parent;
    this.id = id;
    this.socket = socket;
    this.parent.spawn(this::handleInput);
  }

  void sendObject(Object env) {
    try {
      ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
      output.writeObject(env);
    } catch (IOException e) {
      log.error("Failed to send mail to remote node. Couldn't open output stream.", e);
    }
  }

  void handleInput() throws Exception {
    ObjectInputStream objectStream = new ObjectInputStream(socket.getInputStream());
    while (this.socket.isConnected() && !this.socket.isInputShutdown()) {
      Object input = objectStream.readObject();
      if (input instanceof Envelope) {
        ((Envelope) input).send(this.parent);
      } else {
        log.error("Unknown message from {}: {}", this, input);
      }
    }
  }
}
