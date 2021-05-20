package me.evmanu.p2p.operations;

import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.StoredKeyMetadata;

import java.util.List;
import java.util.Map;

public class RepublishOperation implements Operation {

    private final P2PNode node;

    public RepublishOperation(P2PNode node) {
        this.node = node;
    }

    @Override
    public void execute() {

        for (Map.Entry<byte[], StoredKeyMetadata> entries : node.getStoredValues().entrySet()) {
            byte[] key = entries.getKey();

            List<NodeTriple> closestNodes = node.findKClosestNodes(key);

            byte[] value = entries.getValue().getValue();

            for (NodeTriple closestNode : closestNodes) {

                node.getClientManager().performRefreshFor(this.node,  this, closestNode, key,
                        value);

            }
        }
    }

    public void handleRepublishSuccess(NodeTriple triple) {
        this.node.handleSeenNode(triple);
    }

    public void handleFailedRepublish(NodeTriple triple) {
        this.node.handleFailedNodePing(triple);
    }


}
