package me.evmanu.p2p.kademlia;

import lombok.Getter;
import me.evmanu.p2p.grpc.DistLedgerClientManager;
import me.evmanu.p2p.operations.NodeLookupOperation;
import me.evmanu.util.Pair;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListMap;

@Getter
public class P2PNode {

    private final DistLedgerClientManager clientManager;

    private final byte[] nodeID;

    private final ArrayList<ConcurrentLinkedDeque<NodeTriple>> kBuckets = new ArrayList<>(P2PStandards.I);

    private final ArrayList<Long> lastUpdateDone = new ArrayList<>(P2PStandards.I);

    private final Map<Integer, LinkedList<NodeTriple>> nodeWaitList = new ConcurrentSkipListMap<>();

    private final Map<byte[], StoredKeyMetadata> storedValues;

    public P2PNode(byte[] nodeID, DistLedgerClientManager clientManager) {
        this.nodeID = nodeID;
        this.storedValues = new HashMap<>();

        for (int i = 0; i < P2PStandards.I; i++) {
            kBuckets.add(new ConcurrentLinkedDeque<>());
            lastUpdateDone.add(System.currentTimeMillis());
        }

        this.clientManager = clientManager;
    }

    /**
     * Boostrap the node into an existing kademlia network
     * <p>
     * Starts by inserting the boostrap nodes into our k buckets,
     * Then perform a node lookup on our own node ID to populate the our buckets
     */
    public void boostrap() {
        List<NodeTriple> boostrap_nodes = P2PStandards.getBOOSTRAP_NODES();

        for (NodeTriple boostrap_node : boostrap_nodes) {
            int kBucketFor = P2PStandards.getKBucketFor(boostrap_node.getNodeID(), this.getNodeID());

            ConcurrentLinkedDeque<NodeTriple> kBucket = this.kBuckets.get(kBucketFor);

            if (kBucket == null) {
                kBucket = new ConcurrentLinkedDeque<>();
            }

            kBucket.addFirst(boostrap_node);

            this.kBuckets.set(kBucketFor, kBucket);
        }

        new NodeLookupOperation(this, getNodeID(), (_nodes) -> {});
    }

    public void pingHeadOfKBucket(int kBucket) {
        ConcurrentLinkedDeque<NodeTriple> nodeTriples = this.kBuckets.get(kBucket);

        NodeTriple nodeTriple = nodeTriples.peekFirst();

        if (nodeTriple != null) {
            this.clientManager.performPingFor(this, nodeTriple);
        }
    }

    private void appendWaitingNode(int kBucket, NodeTriple nodeTriple) {
        final var bucketWaitList = this.nodeWaitList.getOrDefault(kBucket, new LinkedList<>());

        bucketWaitList.add(nodeTriple);

        this.nodeWaitList.put(kBucket, bucketWaitList);
    }

    private Optional<NodeTriple> popLatestSeenNodeInWaitingList(int kBucket) {
        final var nodesInWait = this.nodeWaitList.get(kBucket);

        if (nodesInWait != null) {
            //Get the most recently seen node from the wait list
            return Optional.of(nodesInWait.removeLast());
        }

        return Optional.empty();
    }

    private Optional<NodeTriple> popOldestSeenNodeInWaitingList(int kBucket) {
        final var nodesInWait = this.nodeWaitList.get(kBucket);

        if (nodesInWait != null) {
            //Get the most recently seen node from the wait list
            return Optional.of(nodesInWait.removeFirst());
        }

        return Optional.empty();
    }

    public void handleFailedNodePing(NodeTriple node) {

        final int kBucketFor = P2PStandards.getKBucketFor(this.nodeID, node.getNodeID());

        final var bucketNodes = kBuckets.get(kBucketFor);

        final var iterator = bucketNodes.iterator();

        boolean removed = false;

        //Remove the node from the K Bucket
        while (iterator.hasNext()) {
            final var currentNode = iterator.next();

            if (Arrays.equals(node.getNodeID(), currentNode.getNodeID())) {
                iterator.remove();

                removed = true;
                break;
            }
        }

        if (removed) {
            //Add the latest seen node to the bucket if the node was present and was removed.
            popLatestSeenNodeInWaitingList(kBucketFor).ifPresent(bucketNodes::addLast);
        }
    }

    public void handleSeenNode(NodeTriple seen) {

        final int kBucketFor = P2PStandards.getKBucketFor(this.nodeID, seen.getNodeID());

        var nodeTriples = this.kBuckets.get(kBucketFor);

        if (nodeTriples == null) {
            nodeTriples = new ConcurrentLinkedDeque<>();
        }

        seen.setLastSeen(System.currentTimeMillis());

        final var iterator = nodeTriples.iterator();

        boolean alreadyPresent = false;

        while (iterator.hasNext()) {
            final var currentNode = iterator.next();

            if (Arrays.equals(seen.getNodeID(), currentNode.getNodeID())) {

                alreadyPresent = true;

                iterator.remove();
                break;
            }
        }

        if (alreadyPresent) {
            //If the node was already present, move it to the tail of the list
            nodeTriples.addLast(seen);

            //Discard of the oldest node in the waiting list, as the ping to the first one was successful
            popOldestSeenNodeInWaitingList(kBucketFor);
        } else {
            if (nodeTriples.size() >= P2PStandards.K) {

                //Ping the head of the list, wait for it's response.
                //If it does not respond, remove it and concatenate this node into the last position of the array
                //If it does respond, put it at the tail of the list and ignore this one
                appendWaitingNode(kBucketFor, seen);

                pingHeadOfKBucket(kBucketFor);

            } else {
                //The node is not present in the K bucket and the bucket is not empty, so add this node to the tail of the list
                nodeTriples.addLast(seen);
            }
        }

        this.kBuckets.set(kBucketFor, nodeTriples);
    }

    public List<NodeTriple> findKClosestNodes(byte[] nodeID) {
        return findClosestNodes(P2PStandards.K, nodeID);
    }

    /**
     * Update the sorted set with the nodes from the given bucket
     * @param maxNodeCount The node limit for the set (Usually {@link P2PStandards#K})
     * @param kBucket The bucket to check
     * @param sortedNodes The node set
     * @param lookupID The ID of the center node
     */
    private void updateSortedNodes(int maxNodeCount, int kBucket, TreeSet<NodeTriple> sortedNodes, byte[] lookupID) {

        ConcurrentLinkedDeque<NodeTriple> nodeTriples = this.kBuckets.get(kBucket);

        if (nodeTriples == null) return;

        for (NodeTriple nodeTriple : nodeTriples) {

            if (sortedNodes.size() < maxNodeCount) {
                sortedNodes.add(nodeTriple);

                continue;
            }

            NodeTriple lastNode = sortedNodes.last();

            BigInteger lastNodeDist = P2PStandards.nodeDistance(lastNode.getNodeID(), lookupID),
                    nodeTripleDist = P2PStandards.nodeDistance(nodeTriple.getNodeID(), lookupID);

            //Seen as the tree set is sorted in ascending order of distance, we compare with the last node
            //If the distance is larger, than it's larger than all the nodes,
            //If not, then we pop the last node and then add the new node

            if (nodeTripleDist.compareTo(lastNodeDist) < 0) {

                //The node triple dist is smaller than the last node
                sortedNodes.pollLast();

                sortedNodes.add(nodeTriple);

            }
        }

    }

    /**
     * Find the closest nodeCount nodes to the nodeID given.
     * @param nodeCount The amount of nodes
     * @param nodeID Node ID
     * @return
     */
    public List<NodeTriple> findClosestNodes(int nodeCount, byte[] nodeID) {

        int kBucketFor = P2PStandards.getKBucketFor(nodeID, this.getNodeID());

        TreeSet<NodeTriple> sortedNodes = new TreeSet<>(new KeyDistanceComparator(nodeID));

        //We start looking at the k bucket that the node ID should be contained in,
        //As that is the bucket that contains the nodes that are closest to it
        //Then we start expanding equally to the left and right buckets
        //Until we have filled the node set.

        for (int i = 0; i < P2PStandards.I; i++) {

            if (kBucketFor + i < P2PStandards.I) {
                updateSortedNodes(nodeCount, kBucketFor + i, sortedNodes, nodeID);
            }

            if (kBucketFor - i >= 0 && i != 0) {
                updateSortedNodes(nodeCount, kBucketFor - i, sortedNodes, nodeID);
            }

            if ((kBucketFor + i > P2PStandards.I && kBucketFor - i < 0) || sortedNodes.size() >= nodeCount) break;

        }

        return new ArrayList<>(sortedNodes);
    }

    public void storeValue(byte[] originNodeID, byte[] ID, byte[] value) {

        if (this.storedValues.containsKey(ID)) {
            StoredKeyMetadata storedKey = this.storedValues.get(ID);

            storedKey.setValue(value);
        } else {
            StoredKeyMetadata storedKey = new StoredKeyMetadata(ID, value, originNodeID);

            this.storedValues.put(ID, storedKey);
        }
    }

    public byte[] loadValue(byte[] ID) {
        return this.storedValues.get(ID).getValue();
    }

}
