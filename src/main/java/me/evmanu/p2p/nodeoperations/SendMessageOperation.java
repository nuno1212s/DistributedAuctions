package me.evmanu.p2p.nodeoperations;

import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;

import java.util.Arrays;
import java.util.List;

public class SendMessageOperation implements Operation {

    private final P2PNode node;

    private final byte[] destinationNode;

    private final byte[] message;

    private NodeTriple triple;

    private volatile boolean finished = false;

    public SendMessageOperation(P2PNode node, byte[] destinationNode, byte[] message) {
        this.node = node;
        this.destinationNode = destinationNode;
        this.message = message;
    }

    @Override
    public void execute() {

        List<NodeTriple> kClosestNodes = node.findKClosestNodes(destinationNode);

        for (NodeTriple kClosestNode : kClosestNodes) {
            if (Arrays.equals(kClosestNode.getNodeID().getBytes(), destinationNode)) {

                this.triple = kClosestNode;

                node.getClientManager().performSendMessage(node, triple, this, message);

                return;
            }
        }

        new NodeLookupOperation(node, destinationNode, (nodes) -> {

            for (NodeTriple nodeTriple : nodes) {
                if (Arrays.equals(nodeTriple.getNodeID().getBytes(), destinationNode)) {

                    this.triple = nodeTriple;

                    node.getClientManager().performSendMessage(node, nodeTriple, this, message);
                    break;
                }
            }
        });
    }

    public void handleSuccessfulResponse() {
        this.node.handleSeenNode(triple);

        finished = true;
    }

    public void handleFailedResponse() {
        this.node.handleFailedNodePing(triple);

        finished = true;
    }

    @Override
    public boolean hasFinished() {
        return finished;
    }
}
