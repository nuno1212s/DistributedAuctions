package me.evmanu.p2p.nodeoperations;

import me.evmanu.p2p.kademlia.KeyDistanceComparator;
import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.P2PStandards;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Perform a node lookup operation for a certain target ID
 * <p>
 * Nodes that are unresponsive are marked as such with the method {@link P2PNode#handleFailedNodePing(NodeTriple)}
 * Nodes that do respond are marked as such with the method {@link P2PNode#handleSeenNode(NodeTriple)}
 */
public class NodeLookupOperation implements Operation {

    private final byte[] lookupID;

    private final P2PNode localNode;

    private final Map<NodeTriple, NodeOperationState> currentOperations;

    private final Map<NodeTriple, Long> waiting_response;

    private final Consumer<List<NodeTriple>> callWhenDone;

    private final AtomicBoolean calledDone = new AtomicBoolean(false);

    private boolean finished = false;

    private ScheduledFuture<?> future;

    public NodeLookupOperation(P2PNode node, byte[] lookupID, Consumer<List<NodeTriple>> callWhenDone) {
        this.localNode = node;
        this.lookupID = lookupID;

        this.currentOperations = new ConcurrentSkipListMap<>(new KeyDistanceComparator(node, true, lookupID));
        this.waiting_response = new ConcurrentSkipListMap<>();

        //Get all the nodes in the buckets, since the currentOperations map is sorted by distance, so we always search the closest
        //This way we can handle if the alpha closest nodes to lookup ID are all offline
        List<NodeTriple> nodes = node.findClosestNodes(Integer.MAX_VALUE, lookupID);

        nodes.forEach(closeNode -> currentOperations.put(closeNode, NodeOperationState.NOT_ASKED));

        this.callWhenDone = callWhenDone != null ? callWhenDone : (_n) -> {};
    }

    @Override
    public void execute() {
        localNode.registerOngoingOperation(this);

        this.future = scheduledExecutor.scheduleAtFixedRate(this::iterate, 1, 1, TimeUnit.SECONDS);

        iterate();
    }

    private void iterate() {
        if (this.ask()) {
            System.out.println("Called this");

            //When we are done, cancel the task
            this.future.cancel(true);

            if (calledDone.compareAndSet(false, true)) {
                this.callWhenDone.accept(this.closestKNodesWithState(NodeOperationState.RESPONDED));

                int kBucket = P2PStandards.getKBucketFor(this.localNode.getNodeID(), this.lookupID);

                this.localNode.kBucketUpdated(kBucket);
            }

            setFinished(true);
        }
    }

    public void setFinished(boolean finished) {
        this.finished = finished;

        if (finished) {
            this.localNode.registerOperationDone(this);
        }
    }

    private synchronized boolean ask() {

        if (this.waiting_response.size() >= P2PStandards.ALPHA) {
            //If we have more than alpha messages in transit, do not ask any more questions
            return false;
        }

        int asked = this.waiting_response.size();

        List<NodeTriple> nodeTriples = closestKNodesWithState(NodeOperationState.NOT_ASKED);

        if (nodeTriples.isEmpty() && waiting_response.isEmpty()) {
            return true;
        }

        System.out.println(nodeTriples);

        for (NodeTriple nodeTriple : nodeTriples) {
            System.out.println("Asking node " + nodeTriple);

            currentOperations.put(nodeTriple, NodeOperationState.WAITING_RESPONSE);

            localNode.getClientManager().performLookupFor(this.localNode, this, nodeTriple, this.lookupID);

            waiting_response.put(nodeTriple, System.currentTimeMillis());

            asked++;

            if (asked >= P2PStandards.ALPHA) {
                //Only allow alpha messages at a time
                break;
            }
        }

        return false;
    }

    @Override
    public boolean hasFinished() {
        return this.finished;
    }

    public void handleFindNodeFailed(NodeTriple targetNode) {
        this.currentOperations.put(targetNode, NodeOperationState.FAILED);

        this.waiting_response.remove(targetNode);

        //Update our local routing table to reflect that this node is not reachable
        this.localNode.handleFailedNodePing(targetNode);

        this.localNode.registerNegativeInteraction(targetNode.getNodeID().getBytes());

        this.iterate();
    }

    public void handleFindNodeReturned(NodeTriple node, List<NodeTriple> foundNodes) {

        this.currentOperations.put(node, NodeOperationState.RESPONDED);

        this.waiting_response.remove(node);

        if (foundNodes.isEmpty()) {
            this.localNode.registerNegativeInteraction(node.getNodeID().getBytes());
        } else {
            this.localNode.registerPositiveInteraction(node.getNodeID().getBytes());
        }

        //Notify our local routing table that we have seen this node
        this.localNode.handleSeenNode(node);

        for (NodeTriple foundNode : foundNodes) {
            //Only put the node in the list if it is not already there,
            //So we don't lose the state of the nodes.
            this.currentOperations.putIfAbsent(foundNode, NodeOperationState.NOT_ASKED);
        }

        this.iterate();
    }

    public List<NodeTriple> closestKNodesWithState(NodeOperationState operationState) {

        List<NodeTriple> nodes = new LinkedList<>();

        /*
        Because this map is sorted by distance to lookupID, we know that the first nodes have the lowest distance from
        The Lookup ID
         */
        for (Map.Entry<NodeTriple, NodeOperationState> node : this.currentOperations.entrySet()) {

            if (node.getValue() != operationState) {
                continue;
            }

            nodes.add(node.getKey());

            if (nodes.size() >= P2PStandards.K) break;
        }

        return nodes;
    }

}
