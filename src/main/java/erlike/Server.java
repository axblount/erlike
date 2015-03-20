package erlike;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

// todo: need a process that will connect to new nodes
// when the parent node asks.
class Server extends Proc {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private ServerSocket serverSocket;

    Server(SocketAddress addr) throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(addr);
    }

    @Override
    protected void main() throws Exception {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());

                Nid ref;
                try {
                    ref = (Nid) inputStream.readObject();
                    outputStream.writeObject(node().getRef());
                } catch (Exception e) {
                    log.error("Failed to confirm connection with new node at {}.",
                            socket.getInetAddress(), e);
                    try {
                        socket.close();
                    } catch (Exception _) {
                    } finally {
                        continue;
                    }
                }

                ref.setAddress(socket.getInetAddress());
                node().registerNode(ref, outputStream);
                node().spawn(this::handleInput, inputStream);
            }
        } finally {
            serverSocket.close();
        }
    }

    private void handleInput(ObjectInputStream inputStream) throws Exception {
        try {
            while (true) {
                try {
                    Envelope input = (Envelope) inputStream.readObject();
                    node().sendById(input.procId, input.message);
                } catch (EOFException e) {
                    return;
                } catch (ClassNotFoundException e) {
                    log.error("Couldn't understand message.", e);
                } catch (Exception e) {
                    log.error("Received an unexpected exception while reading from socket.", e);
                }
            }
        } finally {
            inputStream.close();
        }
    }
}
