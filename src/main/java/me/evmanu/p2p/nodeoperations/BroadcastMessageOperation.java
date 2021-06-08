package me.evmanu.p2p.nodeoperations;

import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.P2PStandards;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;

public class BroadcastMessageOperation implements Operation{

    private final P2PNode center;

    private final int height;

    private final byte[] messageID, messageContent;

    private final Set<NodeTriple> pending_requests;

    private boolean finished = false;

    public BroadcastMessageOperation(P2PNode center, int height, byte[] messageID, byte[] messageContent) {
        this.center = center;
        this.height = height;
        this.messageID = messageID;
        this.messageContent = messageContent;
        this.pending_requests = new ConcurrentSkipListSet<>();
    }

    @Override
    public void execute() {

        this.center.registerOngoingOperation(this);

        Random random = new Random();

        for (int i = height; i < P2PStandards.I; i++) {

            ConcurrentLinkedDeque<NodeTriple> kBucket = this.center.getKBuckets().get(i);

            if (kBucket.isEmpty()) continue;

            List<NodeTriple> nodes = new ArrayList<>(kBucket);

            for (int k = 0; k < P2PStandards.B_K && !nodes.isEmpty(); k++) {

                NodeTriple removedNode = nodes.remove(random.nextInt(nodes.size()));

                this.center.getClientManager().performBroadcastForNode(this.center, this,
                        removedNode, i + 1, messageID, messageContent);

                this.pending_requests.add(removedNode);
            }
        }
    }

    public void handleNodeResponded(NodeTriple triple) {
        this.pending_requests.remove(triple);
        this.center.handleSeenNode(triple);

        if (this.pending_requests.size() == 0) {
            setFinished(true);
        }
    }

    public void handleNodeFailedDelivery(NodeTriple triple) {
        this.pending_requests.remove(triple);
        this.center.handleFailedNodePing(triple);

        if (this.pending_requests.size() == 0) {
            setFinished(true);
        }
    }

    public void setFinished(boolean finished) {
        this.finished = finished;

        if (finished) {
            this.center.registerOperationDone(this);
        }
    }

    @Override
    public boolean hasFinished() {
        return finished;
    }
}
