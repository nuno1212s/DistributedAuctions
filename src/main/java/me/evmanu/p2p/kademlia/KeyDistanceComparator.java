package me.evmanu.p2p.kademlia;

import java.math.BigInteger;
import java.util.Comparator;

/**
 * A comparator to sort nodes by the distance to a certain node
 */
public class KeyDistanceComparator implements Comparator<NodeTriple> {

    private final BigInteger centerNodeID;

    public KeyDistanceComparator(byte[] nodeID) {
        this.centerNodeID = new BigInteger(1, nodeID);
    }

    @Override
    public int compare(NodeTriple o1, NodeTriple o2) {
        if (o1.getNodeID().equals(o2.getNodeID())) {
            return 0;
        }

        BigInteger o1Node = new BigInteger(1, o1.getNodeID().getBytes()),
                o2Node = new BigInteger(1, o2.getNodeID().getBytes());

        return P2PStandards.nodeDistance(o1Node, centerNodeID).compareTo(P2PStandards.nodeDistance(o2Node, centerNodeID));
    }
}
