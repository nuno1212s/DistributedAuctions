package me.evmanu.p2p.kademlia;

import lombok.Getter;
import me.evmanu.util.Pair;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

@Getter
public class P2PNode {

    private final byte[] nodeID;

    private final ArrayList<ConcurrentLinkedDeque<NodeTriple>> kBuckets = new ArrayList<>(P2PStandards.I);

    private final Map<Integer, LinkedList<NodeTriple>> nodeWaitList = new ConcurrentSkipListMap<>();

    private final Map<byte[], byte[]> storedValues;

    public P2PNode(byte[] nodeID) {
        this.nodeID = nodeID;
        this.storedValues = new HashMap<>();

        for (int i = 0; i < P2PStandards.I; i++) {
            kBuckets.add(null);
        }
    }

    public void populateKBuckets() {
        //TODO: Perform a lookup for our own node ID
    }

    private void appendWaitingNode(int kBucket, NodeTriple nodeTriple) {
        final var bucketWaitList = this.nodeWaitList.getOrDefault(kBucket, new LinkedList<>());

        bucketWaitList.add(nodeTriple);

        this.nodeWaitList.put(kBucket, bucketWaitList);
    }

    private Optional<NodeTriple> popLatestSeenNode(int kBucket) {
        final var nodesInWait = this.nodeWaitList.get(kBucket);

        if (nodesInWait != null) {
            //Get the most recently seen node from the wait list
            return Optional.of(nodesInWait.removeLast());
        }

        return Optional.empty();
    }

    private Optional<NodeTriple> popOldestSeenNode(int kBucket) {
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

        //Remove the node from the K Bucket
        while (iterator.hasNext()) {
            final var currentNode = iterator.next();

            if (Arrays.equals(node.getNodeID(), currentNode.getNodeID())) {
                iterator.remove();
                break;
            }
        }

        //Add the latest seen node to the bucket
        popLatestSeenNode(kBucketFor).ifPresent(bucketNodes::addLast);
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

            //Discard of the oldest node in the waiting list
            popOldestSeenNode(kBucketFor);
        } else {
            if (nodeTriples.size() >= P2PStandards.K) {

                //Ping the head of the list, wait for it's response.
                //If it does not respond, remove it and concatenate this node into the last position of the array
                //If it does respond, put it at the tail of the list and ignore this one
                appendWaitingNode(kBucketFor, seen);

                //TODO: Ping the head of the list

            } else {
                //The node is not present in the K bucket and the bucket is not empty, so add this node to the tail of the list
                nodeTriples.addLast(seen);
            }
        }

        this.kBuckets.set(kBucketFor, nodeTriples);
    }

    public List<NodeTriple> findNode(byte[] nodeID) {

        LinkedList<Pair<NodeTriple, BigInteger>> KClosestNodes = new LinkedList<>();

        for (ConcurrentLinkedDeque<NodeTriple> bucket : this.kBuckets) {
            for (NodeTriple node : bucket) {

                final var distance = P2PStandards.nodeDistance(nodeID, node.getNodeID());

                if (KClosestNodes.isEmpty()) {
                    KClosestNodes.addLast(Pair.of(node, distance));
                } else {
                    final var iterator = KClosestNodes.listIterator(KClosestNodes.size());

                    int currentIndex = KClosestNodes.size();

                    boolean inserted = false;

                    while (iterator.hasPrevious()) {

                        final var previous = iterator.previous();

                        if (previous.getValue().compareTo(distance) >= 0) {
                            //When previous is larger than the current distance, then we need to add the node
                            //At this position.
                            //(If the current position is >= than K, then it's not part of the K first nodes)
                            if (currentIndex < P2PStandards.K) {
                                iterator.add(Pair.of(node, distance));

                                inserted = true;
                            }

                            break;
                        }

                        currentIndex--;
                    }

                    if (!inserted && KClosestNodes.peekFirst().getValue().compareTo(distance) <= 0) {
                        KClosestNodes.addFirst(Pair.of(node, distance));

                        while (KClosestNodes.size() > P2PStandards.K) {
                            KClosestNodes.removeLast();
                        }
                    }
                }
            }
        }

        return KClosestNodes.stream().map(Pair::getKey).collect(Collectors.toList());
    }

    public void storeValue(byte[] ID, byte[] value) {
        this.storedValues.put(ID, value);
    }

    public byte[] loadValue(byte[] ID) {
        return this.storedValues.get(ID);
    }

}
