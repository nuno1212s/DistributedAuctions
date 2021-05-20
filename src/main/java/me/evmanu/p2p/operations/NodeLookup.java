package me.evmanu.p2p.operations;

import me.evmanu.p2p.kademlia.KeyDistanceComparator;
import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.P2PStandards;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Perform a node lookup operation for a certain target ID
 *
 * Nodes that are unresponsive are marked as such with the method {@link P2PNode#handleFailedNodePing(NodeTriple)}
 * Nodes that do respond are marked as such with the method {@link P2PNode#handleSeenNode(NodeTriple)}
 */
public class NodeLookup {

    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    private final byte[] lookupID;

    private final P2PNode localNode;

    private final Map<NodeTriple, NodeOperationState> currentOperations;

    private final Map<NodeTriple, Long> waiting_response;

    private final ScheduledFuture<?> future;

    public NodeLookup(P2PNode node, byte[] lookupID) {
        this.localNode = node;
        this.lookupID = lookupID;

        this.currentOperations = new ConcurrentSkipListMap<>(new KeyDistanceComparator(lookupID));
        this.waiting_response = new ConcurrentSkipListMap<>();

        List<NodeTriple> nodes = node.findClosestNodes(P2PStandards.ALPHA, lookupID);

        nodes.forEach(closeNode -> currentOperations.put(closeNode, NodeOperationState.NOT_ASKED));

        this.future = scheduledExecutor.scheduleAtFixedRate(this::execute, 50, 50, TimeUnit.MILLISECONDS);

        execute();
    }

    public void execute() {

        if (this.ask()) {
            //When we are done, cancel the task
            this.future.cancel(true);
        }

    }

    public boolean ask() {

        if (this.waiting_response.size() >= P2PStandards.ALPHA) {
            //If we have more than alpha messages in transit, do not ask any more questions
            return false;
        }

        int asked = this.waiting_response.size();

        List<NodeTriple> nodeTriples = closestKNodesWithState(NodeOperationState.NOT_ASKED);

        if (nodeTriples.isEmpty() && waiting_response.isEmpty()) {
            return true;
        }

        for (NodeTriple nodeTriple : nodeTriples) {
            localNode.getClientManager().performLookupFor(this.localNode, this, nodeTriple, this.lookupID);

            currentOperations.put(nodeTriple, NodeOperationState.WAITING_RESPONSE);

            waiting_response.put(nodeTriple, System.currentTimeMillis());

            asked++;

            if (asked >= P2PStandards.ALPHA) {
                //Only allow alpha messages at a time
                break;
            }
        }

        return false;
    }

    public void handleFindNodeFailed(NodeTriple targetNode) {
        this.currentOperations.put(targetNode, NodeOperationState.FAILED);

        this.waiting_response.remove(targetNode);

        //Update our local routing table to reflect that this node is not reachable
        this.localNode.handleFailedNodePing(targetNode);
    }

    public void handleFindNodeReturned(NodeTriple node, List<NodeTriple> foundNodes) {

        this.currentOperations.put(node, NodeOperationState.RESPONDED);

        this.waiting_response.remove(node);

        //Notify our local routing table that we have seen this node
        this.localNode.handleSeenNode(node);

        for (NodeTriple foundNode : foundNodes) {
            //Only put the node in the list if it is not already there,
            //So we don't lose the state of the nodes.
            this.currentOperations.putIfAbsent(foundNode, NodeOperationState.NOT_ASKED);
        }

        this.ask();
    }

    public List<NodeTriple> closestKNodesWithState(NodeOperationState operationState) {
        List<NodeTriple> nodes = new LinkedList<>();

        /*
        Because this map is sorted by distance to lookupID, we know that the first nodes have the lowest distance from
        The Lookup ID
         */
        for (Map.Entry<NodeTriple, NodeOperationState> node : this.currentOperations.entrySet()) {

            if (node.getValue() == operationState) {
                continue;
            }

            nodes.add(node.getKey());

            if (nodes.size() >= P2PStandards.K) break;
        }

        return nodes;
    }

}
