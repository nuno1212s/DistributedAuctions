package me.evmanu.p2p.operations;

import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.StoredKeyMetadata;

public class StoreOperation implements StoreOperationBase {

    private final P2PNode center;
    private final StoredKeyMetadata metadata;

    public StoreOperation(P2PNode center, StoredKeyMetadata metadata) {
        this.center = center;
        this.metadata = metadata;
    }

    @Override
    public void execute() {

        new NodeLookupOperation(center, this.metadata.getKey(), (nodes) -> {

            for (NodeTriple destination : nodes) {
                center.getClientManager().performStoreFor(center, this, destination, this.metadata);
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
