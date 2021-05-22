package me.evmanu.p2p.kademlia;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.evmanu.util.ByteWrapper;
import me.evmanu.util.Hex;

import java.net.InetAddress;
import java.util.Arrays;

@Getter
@AllArgsConstructor
public class NodeTriple implements Comparable<NodeTriple> {

    private final InetAddress ipAddress;

    private final int udpPort;

    private final ByteWrapper nodeID;

    @Setter
    private long lastSeen;

    public NodeTriple(InetAddress ipAddress, int udpPort, byte[] nodeID, long lastSeen) {
        this.ipAddress = ipAddress;
        this.udpPort = udpPort;
        this.nodeID = new ByteWrapper(nodeID);
        this.lastSeen = lastSeen;
    }

    @Override
    public int compareTo(NodeTriple o) {
        return Arrays.compare(nodeID.getBytes(), o.getNodeID().getBytes());
    }

    @Override
    public String toString() {
        return "NodeTriple{" +
                "ipAddress=" + ipAddress +
                ", udpPort=" + udpPort +
                ", nodeID=" + Hex.toHexString(nodeID.getBytes()) +
                ", lastSeen=" + lastSeen +
                '}';
    }
}