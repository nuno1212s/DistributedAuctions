package me.evmanu.p2p.nodeoperations;

import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.P2PStandards;
import me.evmanu.p2p.kademlia.StoredKeyMetadata;
import me.evmanu.util.ByteWrapper;

import java.util.List;
import java.util.Map;

public class OriginalContentRepublishOperation implements StoreOperationBase{

    private final P2PNode node;

    public OriginalContentRepublishOperation(P2PNode node) {
        this.node = node;
    }

    @Override
    public void execute() {

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
    }

    @Override
    public void handleSuccessfulStore(NodeTriple triple) {
        this.node.handleSeenNode(triple);
    }

    @Override
    public void handleFailedStore(NodeTriple triple) {
        this.node.handleFailedNodePing(triple);
    }
}
