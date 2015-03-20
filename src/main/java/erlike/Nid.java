package erlike;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;

public class Nid implements Serializable {
    private final UUID uuid;
    private final String name;
    private transient InetAddress address;

    Nid(UUID uuid, String name, InetAddress address) {
        this.uuid = uuid;
        this.name = name;
        this.address = address;
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public InetAddress getAddress() {
        return address;
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
