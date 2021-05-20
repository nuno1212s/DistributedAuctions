package me.evmanu.p2p.operations;

import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;

public class StoreOperation implements Operation {

    private final P2PNode center;
    private final byte[] key, value;

    public StoreOperation(P2PNode center, byte[] key, byte[] value) {
        this.center = center;
        this.key = key;
        this.value = value;
    }

    @Override
    public void execute() {

        new NodeLookupOperation(center, key, (nodes) -> {

            for (NodeTriple destination : nodes) {
                center.getClientManager().performStoreFor(center, this, destination, key, value);
            }

        });

    }

    public void handleSuccessfulStore(NodeTriple triple) {
        this.center.handleSeenNode(triple);
    }

    public void handleFailedStore(NodeTriple triple) {
        this.center.handleFailedNodePing(triple);
    }

}
