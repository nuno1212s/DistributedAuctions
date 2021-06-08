package me.evmanu.p2p.nodeoperations;

import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.P2PStandards;
import me.evmanu.p2p.kademlia.StoredKeyMetadata;
import me.evmanu.util.ByteWrapper;

import java.util.List;
import java.util.Map;

/**
 * Republish the content that is owned by us in the P2P network
 */
public class OriginalContentRepublishOperation implements StoreOperationBase {

    private final P2PNode node;

    private boolean finished = false;

    public OriginalContentRepublishOperation(P2PNode node) {
        this.node = node;
    }

    @Override
    public void execute() {

        node.registerOngoingOperation(this);

        Map<ByteWrapper, StoredKeyMetadata> publishedValues = node.getPublishedValues();

        publishedValues.forEach((key, metadata) -> {

            if (System.currentTimeMillis() - metadata.getLastRepublished() < P2PStandards.T_REPUBLISH) {
                return;
            }

            metadata.registerRepublished();

            List<NodeTriple> closestNodes = this.node.findKClosestNodes(key.getBytes());

            for (NodeTriple closestNode : closestNodes) {
                this.node.getClientManager().performStoreFor(this.node, this, closestNode, metadata);
            }

        });

        setFinished(true);
    }

    @Override
    public void handleSuccessfulStore(NodeTriple triple) {
        this.node.handleSeenNode(triple);
    }

    @Override
    public void handleFailedStore(NodeTriple triple) {
        this.node.handleFailedNodePing(triple);
    }

    public void setFinished(boolean finished) {
        this.finished = finished;

        if (finished) {
            this.node.registerOperationDone(this);
        }
    }

    @Override
    public boolean hasFinished() {
        return this.finished;
    }
}
