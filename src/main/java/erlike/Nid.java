package erlike;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;

public final class Nid implements Serializable {
    public final UUID uuid;
    public final String name;
    private transient InetAddress address;

    Nid(Node node) {
        this.uuid = node.getUUID();
        this.name = node.getName();
        this.address = InetAddress.getLoopbackAddress();
    }

    void setAddress(InetAddress address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Nid)
            return this.uuid == ((Nid)other).uuid;
        return false;
    }

    @Override
    public String toString() {
        if (address != null && !address.equals(InetAddress.getLoopbackAddress()))
            return String.format("%s@%s", name, address.toString());
        else
            return name;
    }
}
