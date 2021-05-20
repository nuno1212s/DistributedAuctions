package me.evmanu.p2p.kademlia;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.net.InetAddress;
import java.util.Arrays;

@Getter
@AllArgsConstructor
public class NodeTriple implements Comparable<NodeTriple> {

    private final InetAddress ipAddress;

    private final int udpPort;

    private final byte[] nodeID;

    @Setter
    private long lastSeen;

    @Override
    public int compareTo(NodeTriple o) {
        return Arrays.compare(nodeID, o.getNodeID());
    }
}