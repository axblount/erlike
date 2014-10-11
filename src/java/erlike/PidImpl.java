package erlike;

/*package-local*/ final class PidImpl implements Pid {
    private final Node contextNode;
    private final int nodeId;
    private final int procId;

    PidImpl(final Node contextNode, final int nodeId, final int procId) {
        this.contextNode = contextNode;
        this.nodeId = nodeId;
        this.procId = procId;
    }

    @Override public void send(Object msg) {
        contextNode.sendById(nodeId, procId, msg);
    }

    @Override public boolean equals(Object other) {
        if (other instanceof PidImpl) {
            PidImpl pid = (PidImpl)other;
            return this.procId == pid.procId && this.nodeId == pid.nodeId;
        }
        return false;
    }

    @Override public int hashCode() {
        return nodeId ^ procId;
    }
}
