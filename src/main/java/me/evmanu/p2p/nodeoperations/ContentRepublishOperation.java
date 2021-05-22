package me.evmanu.p2p.nodeoperations;

import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.P2PStandards;
import me.evmanu.p2p.kademlia.StoredKeyMetadata;
import me.evmanu.util.ByteWrapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Republish the content that is contained in this node to the other nodes in the network
 */
public class ContentRepublishOperation implements StoreOperationBase {

    private final P2PNode node;

    private boolean finished = false;

    public ContentRepublishOperation(P2PNode node) {
        this.node = node;
    }

    @Override
    public void execute() {

        this.node.registerOngoingOperation(this);

        Map<ByteWrapper, StoredKeyMetadata> storedValues = this.node.getStoredValues();

        storedValues.forEach((key, stored) -> {

            //We only find the K closest nodes in our routing table, we don't perform
            //A node lookup as that would be extremely expensive to do for all stored values
            List<NodeTriple> kClosestNodes = node.findKClosestNodes(key.getBytes());

            if (System.currentTimeMillis() - stored.getLastRepublished() < P2PStandards.T_REPLICATE) {
                //Check that the node has to be republished

                return;
            }

            stored.registerRepublished();

            for (NodeTriple kClosestNode : kClosestNodes) {

                if (Arrays.equals(kClosestNode.getNodeID().getBytes(), this.node.getNodeID())) {
                    //We don't need to republish for ourselves
                    continue;
                }

                node.getClientManager().performStoreFor(this.node, this, kClosestNode,
                        stored);
            }

        });

        setFinished(true);
    }

    public void setFinished(boolean finished) {
        this.finished = finished;

        if (finished) {
            this.node.registerOperationDone(this);
        }
    }

    @Override
    public boolean hasFinished() {
        return finished;
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
