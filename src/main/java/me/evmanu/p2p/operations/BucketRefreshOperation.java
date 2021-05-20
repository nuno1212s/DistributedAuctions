package me.evmanu.p2p.operations;

import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;

import java.util.concurrent.ConcurrentLinkedDeque;

public class BucketRefreshOperation implements Operation {

    private int kBucket;

    private P2PNode node;

    public BucketRefreshOperation(int kBucket, P2PNode node) {

        this.kBucket = kBucket;
        this.node = node;

    }

    @Override
    public void execute() {

        ConcurrentLinkedDeque<NodeTriple> nodeTriples = node.getKBuckets().get(kBucket);

        if (nodeTriples.isEmpty()) return;

        NodeTriple nodeTriple = nodeTriples.peekLast();

        NodeLookupOperation lookupOperation = new NodeLookupOperation(node, nodeTriple.getNodeID(), (_nodes) -> {

        });

        lookupOperation.execute();

    }
}
