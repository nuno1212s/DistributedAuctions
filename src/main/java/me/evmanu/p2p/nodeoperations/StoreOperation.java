package me.evmanu.p2p.nodeoperations;

import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.StoredKeyMetadata;

public class StoreOperation implements StoreOperationBase {

    private final P2PNode center;
    private final StoredKeyMetadata metadata;

    private boolean finished = false;

    public StoreOperation(P2PNode center, StoredKeyMetadata metadata) {
        this.center = center;
        this.metadata = metadata;
    }

    @Override
    public void execute() {

        this.center.registerOngoingOperation(this);

        new NodeLookupOperation(center, this.metadata.getKey(), (nodes) -> {

            for (NodeTriple destination : nodes) {
                center.getClientManager().performStoreFor(center, this, destination, this.metadata);
            }

            setFinished(true);
        });

    }

    public void setFinished(boolean finished) {
        this.finished = finished;

        if (finished) {
            this.center.registerOperationDone(this);
        }
    }

    @Override
    public boolean hasFinished() {
        return this.finished;
    }

    public void handleSuccessfulStore(NodeTriple triple) {
        this.center.handleSeenNode(triple);
    }

    public void handleFailedStore(NodeTriple triple) {
        this.center.handleFailedNodePing(triple);
    }

}
