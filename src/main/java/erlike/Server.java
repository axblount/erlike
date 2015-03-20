package erlike;

import org.slf4j.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

import static erlike.Library.*;

// todo: need a process that will connect to new nodes
// when the parent node asks.
class Server {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private Node node;
    private ServerSocket serverSocket;
    private ConcurrentMap<Nid, Socket> connections;

    Server(Node node, SocketAddress addr) throws IOException {
        this.node = node;
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(addr);
        connections = new ConcurrentHashMap<>();
    }

    private void listener() throws Exception {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());

                Nid nid;
                try {
                    nid = (Nid) inputStream.readObject();
                    outputStream.writeObject(node.getRef());
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

                nid.setAddress(socket.getInetAddress());
                node.registerNode(nid, outputStream);
                node.spawn(this::handleInput, inputStream);
            }
        } finally {
            serverSocket.close();
        }
    }

    private void handleOutput() throws Exception {
        while (true) {
            receive(obj -> {
                if (obj instanceof Envelope) {
                    Envelope env = (Envelope)obj;
                    Socket socket = connections.get(env.pid.getNid());
                    if (socket == null)
                        return; // TODO: retry connection!
                    if (!(socket.isClosed() || socket.isOutputShutdown())) {
                        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                        outputStream.writeObject(env);
                    }
                } // else ignore it
            });
        }
    }

    private void handleInput(ObjectInputStream inputStream) throws Exception {
        try {
            while (true) {
                try {
                    Envelope input = (Envelope) inputStream.readObject();
                    node.sendById(input.pid, input.message);
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

    public Nid connect(URL url) throws IOException {
        InetAddress address = InetAddress.getByName(url.getHost());
        int port = url.getPort();
        if (port == -1) {
            if (!url.getProtocol().equals("erlike"))
                port = 13331;
            else
                port = url.getDefaultPort();
        }
        Socket socket = null;
        try {
            socket = new Socket(address, port);

            ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());

            outputStream.writeObject(node.getRef());
            Nid nid = (Nid) inputStream.readObject();

            node.spawn(this::handleInput, inputStream);

            node.registerNode(nid, outputStream);

            return nid;
        } catch (Exception e) {
            if (socket != null)
                socket.close();
        }
    }
}
