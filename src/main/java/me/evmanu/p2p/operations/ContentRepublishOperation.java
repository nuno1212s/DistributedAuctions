package me.evmanu.p2p.operations;

import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.P2PStandards;
import me.evmanu.p2p.kademlia.StoredKeyMetadata;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ContentRepublishOperation implements StoreOperationBase {

    private final P2PNode node;

    public ContentRepublishOperation(P2PNode node) {
        this.node = node;
    }

    @Override
    public void execute() {

        Map<byte[], StoredKeyMetadata> storedValues = this.node.getStoredValues();

        storedValues.forEach((key, stored) -> {

            //We only find the K closest nodes in our routing table, we don't perform
            //A node lookup as that would be extremely expensive to do for all stored values
            List<NodeTriple> kClosestNodes = node.findKClosestNodes(key);

            if (System.currentTimeMillis() - stored.getLastRepublished() < P2PStandards.T_REPLICATE) {
                //Check that the node has to be republished

                return;
            }

            stored.registerRepublished();

            for (NodeTriple kClosestNode : kClosestNodes) {

                if (Arrays.equals(kClosestNode.getNodeID(), this.node.getNodeID())) {
                    //We don't need to republish for ourselves
                    continue;
                }

                node.getClientManager().performStoreFor(this.node, this, kClosestNode,
                        stored);
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
