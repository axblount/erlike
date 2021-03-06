package erlike;

class Server {
    /*
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private Node node;
    private ServerSocket serverSocket;
    private ConcurrentMap<NodeRef, Socket> connections;

    private ProcRef listener, outgoing;

    Server(Node node, SocketAddress addr) throws IOException {
        this.node = node;
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(addr);
        connections = new ConcurrentHashMap<>();

        listener = node.spawn(this::listener);
        outgoing = node.spawn(this::outgoing);
    }

    public void sendOutgoingMail(ProcRef pid, Object msg) {
        outgoing.send(new Envelope(pid, msg));
    }

    private void listener() throws Exception {
        try {
            while (!serverSocket.isClosed() && !Thread.interrupted()) {
                Socket socket = serverSocket.accept();
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());

                NodeRef nid;
                try {
                    nid = (NodeRef) inputStream.readObject();
                    outputStream.writeObject(node.self());
                } catch (Exception e) {
                    log.error("Failed to confirm connection with new node at {}.",
                            socket.getInetAddress(), e);
                    try {
                        socket.close();
                    } catch (Exception ee) {
                        log.error("Failed to close socket after failed handshake.", ee);
                    }

                    continue;
                }

                nid.setAddress(socket.getInetAddress());
                node.spawn(this::incoming, inputStream);
            }
        } finally {
            serverSocket.close();
        }
    }

    private void outgoing() throws Exception {
        while (true) {
            receive(new CaseConsumer().match(Envelope.class, (env) -> {
                Socket socket = connections.get(env.pid.getNodeId());
                if (socket == null)
                    return; // TODO: retry connection!
                if (!(socket.isClosed() || socket.isOutputShutdown())) {
                    ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                    outputStream.writeObject(env);
                }
            }).otherwise((obj) -> {
                log.error("Tried to send a non-Envelope: {}", obj);
            }));
        }
    }

    private void incoming(ObjectInputStream inputStream) throws Exception {
        try {
            while (true) {
                try {
                    Envelope input = (Envelope) inputStream.readObject();
                    node.send(input.pid, input.message);
                } catch (EOFException e) {
                    return;
                } catch (ClassCastException e) {
                    log.error("Received non-Envelope message.", e);
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

    public NodeRef connect(URL url) throws IOException {
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

            outputStream.writeObject(node.self());
            NodeRef nid = (NodeRef) inputStream.readObject();

            node.spawn(this::incoming, inputStream);

            return nid;
        } catch (Exception e) {
            if (socket != null)
                socket.close();
        }
        return null;
    }
    */
}
