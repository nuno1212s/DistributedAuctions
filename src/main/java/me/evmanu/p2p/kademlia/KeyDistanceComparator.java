package me.evmanu.p2p.kademlia;

import java.math.BigInteger;
import java.util.Comparator;

/**
 * A comparator to sort nodes by the distance to a certain node
 */
public class KeyDistanceComparator implements Comparator<NodeTriple> {

    private final BigInteger centerNodeID;

    private P2PNode issuer;

    private final boolean useTrust;

    public KeyDistanceComparator(byte[] nodeID) {
        this.centerNodeID = new BigInteger(1, nodeID);
        this.useTrust = false;
    }

    public KeyDistanceComparator(P2PNode node, boolean useTrust, byte [] nodeID) {
        this.centerNodeID = new BigInteger(1, nodeID);
        this.issuer = node;
        this.useTrust = useTrust;
    }

    @Override
    public int compare(NodeTriple o1, NodeTriple o2) {
        if (o1.getNodeID().equals(o2.getNodeID())) {
            return 0;
        }

        BigInteger o1Node = new BigInteger(1, o1.getNodeID().getBytes()),
                o2Node = new BigInteger(1, o2.getNodeID().getBytes());

        BigInteger nodeDistance1 = P2PStandards.nodeDistance(o1Node, centerNodeID);

        BigInteger nodeDistance2 = P2PStandards.nodeDistance(o2Node, centerNodeID);

        if (useTrust && issuer != null) {
            nodeDistance1 = P2PTrust.calculateNewDistance(nodeDistance1, issuer, o1.getNodeID().getBytes());
            nodeDistance2 = P2PTrust.calculateNewDistance(nodeDistance2, issuer, o2.getNodeID().getBytes());
        }

        return nodeDistance1.compareTo(nodeDistance2);
    }
}
