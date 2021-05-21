package me.evmanu.p2p.operations;

import me.evmanu.p2p.kademlia.KeyDistanceComparator;
import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.P2PStandards;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ContentLookupOperation implements Operation {

    private final P2PNode centerNode;

    private final byte[] targetID;

    private final Map<NodeTriple, NodeOperationState> storedNodes;

    private final Map<NodeTriple, Long> pending_requests;

    private final AtomicBoolean isContentFound = new AtomicBoolean(false);

    private final Consumer<byte[]> resultConsumer;

    private ScheduledFuture<?> future;

    private byte[] foundValue;

    public ContentLookupOperation(P2PNode centerNode, byte[] targetID, Consumer<byte[]> resultConsumer) {

        this.centerNode = centerNode;
        this.targetID = targetID;

        this.storedNodes = new ConcurrentSkipListMap<>(new KeyDistanceComparator(this.targetID));

        List<NodeTriple> closestNodes = this.centerNode.findClosestNodes(Integer.MAX_VALUE, targetID);

        closestNodes.forEach(node -> this.storedNodes.put(node, NodeOperationState.NOT_ASKED));

        this.pending_requests = new ConcurrentSkipListMap<>();
        this.resultConsumer = resultConsumer;
    }

    @Override
    public void execute() {

        future = Operation.scheduledExecutor.scheduleAtFixedRate(this::iterate, 50, 50, TimeUnit.MILLISECONDS);

        iterate();

    }

    private void iterate() {

        if (ask()) {

            this.future.cancel(true);

        }

    }

    private boolean ask() {

        if (this.isContentFound.get()) {
            return true;
        }

        if (this.pending_requests.size() >= P2PStandards.ALPHA) {
            return false;
        }

        int asked = this.pending_requests.size();

        List<NodeTriple> nodes = closestKNodesWithState(NodeOperationState.NOT_ASKED);

        if (nodes.isEmpty() && pending_requests.isEmpty()) {
            return true;
        }

        for (NodeTriple nodeTriple : nodes) {
            this.centerNode.getClientManager().findValueFromNode(this.centerNode, this, nodeTriple, this.targetID);

            storedNodes.put(nodeTriple, NodeOperationState.WAITING_RESPONSE);

            pending_requests.put(nodeTriple, System.currentTimeMillis());

            asked++;

            if (asked >= P2PStandards.ALPHA) {
                //Only allow alpha messages at a time
                break;
            }
        }

        return false;
    }

    public void handleFoundValue(NodeTriple triple, byte[] value) {

        this.foundValue = value;

        this.isContentFound.set(true);

        this.pending_requests.remove(triple);

        this.centerNode.handleSeenNode(triple);

        this.resultConsumer.accept(this.foundValue);
    }

    public void nodeReturnedMoreNodes(NodeTriple node, Collection<NodeTriple> nodeTriples) {

        this.pending_requests.remove(node);

        this.centerNode.handleSeenNode(node);

        for (NodeTriple nodeTriple : nodeTriples) {
            this.storedNodes.putIfAbsent(nodeTriple, NodeOperationState.NOT_ASKED);
        }

    }

    public void nodeFailedLookup(NodeTriple triple) {
        this.storedNodes.put(triple, NodeOperationState.FAILED);

        this.pending_requests.remove(triple);

        this.centerNode.handleFailedNodePing(triple);
    }

    public List<NodeTriple> closestKNodesWithState(NodeOperationState operationState) {

        List<NodeTriple> nodes = new LinkedList<>();

        /*
        Because this map is sorted by distance to lookupID, we know that the first nodes have the lowest distance from
        The Lookup ID
         */
        for (Map.Entry<NodeTriple, NodeOperationState> node : this.storedNodes.entrySet()) {

            if (node.getValue() != operationState) {
                continue;
            }

            nodes.add(node.getKey());

            if (nodes.size() >= P2PStandards.K) break;
        }

        return nodes;
    }


}
