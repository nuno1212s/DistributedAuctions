package me.evmanu.p2p;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;

@Getter
public class P2PNode {

    private final byte[] nodeID;

    private ArrayList<ArrayList<NodeTriple>> kBuckets = new ArrayList<>(P2PStandards.I);

    public P2PNode(byte[] nodeID) {
        this.nodeID = nodeID;

        for (int i = 0; i < P2PStandards.I; i++) {
            kBuckets.add(null);
        }
    }

    public void handleSeenNode(NodeTriple seen) {

        final var kBucketFor = P2PStandards.getKBucketFor(this.nodeID, seen.getNodeID());

        var nodeTriples = this.kBuckets.get(kBucketFor);

        if (nodeTriples == null) {
            nodeTriples = new ArrayList<>(P2PStandards.K);
        }

        final var iterator = nodeTriples.iterator();

        boolean alreadyPresent = false;

        while (iterator.hasNext()) {
            final var currentNode = iterator.next();

            if (Arrays.equals(seen.getNodeID(), currentNode.getNodeID())) {

                alreadyPresent = true;

                iterator.remove();
                break;

            }
        }

        if (alreadyPresent) {
            nodeTriples.add(seen);
        } else {
            if (nodeTriples.size() >= P2PStandards.K) {

                //Ping the head of the list, wait for it's response.
                //If it does not respond, remove it and concatenate this node into the last position of the array
                //If it does respond, put it at the tail of the list and ignore this one

            } else {
                nodeTriples.add(seen);
            }
        }

        this.kBuckets.set(kBucketFor, nodeTriples);
    }


}
