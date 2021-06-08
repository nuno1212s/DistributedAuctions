package me.evmanu.p2p.kademlia;

import lombok.Getter;
import me.evmanu.p2p.grpc.DistLedgerClientManager;
import me.evmanu.p2p.nodeoperations.*;
import me.evmanu.util.ByteWrapper;
import me.evmanu.util.Hex;
import me.evmanu.util.Pair;
import org.w3c.dom.Node;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class P2PNode {

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Getter
    private final byte[] nodeID;

    @Getter
    private final int nodePublicPort;

    @Getter
    private final DistLedgerClientManager clientManager;

    @Getter
    private final ArrayList<ConcurrentLinkedDeque<NodeTriple>> kBuckets;

    private final ArrayList<Long> lastKBucketUpdate;

    /**
     * Lock each bucket individually
     * Each bucket still uses a concurrent linked deque so we can still iterate through the nodes in the
     * bucket while it's being changed, but we can only perform one change at once.
     */
    private final ArrayList<ReentrantLock> kBucketWriteLocks;

    private final Map<Integer, LinkedList<NodeTriple>> nodeWaitList;

    @Getter
    private final Map<ByteWrapper, StoredKeyMetadata> storedValues, publishedValues;

    private final Map<ByteWrapper, Pair<Long, Long>> storedCRCs;

    private final Map<ByteWrapper, Long> requestedCRCs;

    private final Map<ByteWrapper, Pair<Integer, Integer>> interactionsPerPeer;

    private final Set<ByteWrapper> seenMessages;

    private final List<Operation> onGoingOperations;

    private long lastRepublish, lastOriginalRepublish;

    public P2PNode(byte[] nodeID, DistLedgerClientManager clientManager, int port) {
        this.nodeID = nodeID;
        this.nodePublicPort = port;
        this.kBuckets = new ArrayList<>(P2PStandards.I);
        this.lastKBucketUpdate = new ArrayList<>(P2PStandards.I);
        this.kBucketWriteLocks = new ArrayList<>(P2PStandards.I);
        this.nodeWaitList = new ConcurrentSkipListMap<>();
        this.storedValues = new ConcurrentSkipListMap<>();
        this.publishedValues = new ConcurrentSkipListMap<>();
        this.storedCRCs = new ConcurrentSkipListMap<>();
        this.requestedCRCs = new ConcurrentSkipListMap<>();
        this.interactionsPerPeer = new ConcurrentSkipListMap<>();
        this.seenMessages = new ConcurrentSkipListSet<>();
        this.onGoingOperations = Collections.synchronizedList(new LinkedList<>());

        this.lastRepublish = System.currentTimeMillis();
        this.lastOriginalRepublish = System.currentTimeMillis();

        for (int i = 0; i < P2PStandards.I; i++) {
            kBuckets.add(new ConcurrentLinkedDeque<>());
            kBucketWriteLocks.add(new ReentrantLock());
            lastKBucketUpdate.add(System.currentTimeMillis());
        }

        this.clientManager = clientManager;

        executor.scheduleAtFixedRate(this::iterate, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Boostrap the node into an existing kademlia network
     * <p>
     * Starts by inserting the boostrap nodes into our k buckets,
     * Then perform a node lookup on our own node ID to populate the our buckets
     */
    public void boostrap(List<NodeTriple> boostrap_nodes) {

        for (NodeTriple boostrap_node : boostrap_nodes) {
            int kBucketFor = P2PStandards.getKBucketFor(boostrap_node.getNodeID().getBytes(), this.getNodeID());

            ConcurrentLinkedDeque<NodeTriple> kBucket = this.kBuckets.get(kBucketFor);

            if (kBucket == null) {
                kBucket = new ConcurrentLinkedDeque<>();
            }

            kBucket.addFirst(boostrap_node);

            System.out.println("Populated k bucket " + kBucketFor + " with node " + boostrap_node);

            this.kBuckets.set(kBucketFor, kBucket);
        }

        System.out.println("Populated k Buckets with the boostrap nodes, executing lookup on our own node ID.");

        new NodeLookupOperation(this, getNodeID(), (_nodes) -> {
        }).execute();
    }

    private void pingHeadOfKBucket(int kBucket) {
        ConcurrentLinkedDeque<NodeTriple> nodeTriples = this.kBuckets.get(kBucket);

        NodeTriple nodeTriple = nodeTriples.peekFirst();

        if (nodeTriple != null) {
            this.clientManager.performPingFor(this, nodeTriple);
        }
    }

    private boolean hasSolvedCRC(NodeTriple triple) {
        return this.storedCRCs.containsKey(triple.getNodeID());
    }

    public void storeCRCResponse(NodeTriple triple, long challenge, long response) {
        this.storedCRCs.put(triple.getNodeID(), Pair.of(challenge, response));
    }

    public void storeCRCRequest(NodeTriple triple, long challenge) {
        this.requestedCRCs.put(triple.getNodeID(), challenge);
    }

    private void requestCRCFromNode(NodeTriple triple) {

        if (this.requestedCRCs.containsKey(triple.getNodeID())) {
            System.out.println("Node " + triple + " already performed CRC.");
            return;
        }

        new RequestCRCOperation(this, triple).execute();
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

        final int kBucketFor = P2PStandards.getKBucketFor(this.nodeID, node.getNodeID().getBytes());

        ReentrantLock lock = kBucketWriteLocks.get(kBucketFor);

        try {
            lock.lock();

            final var bucketNodes = kBuckets.get(kBucketFor);

            final var iterator = bucketNodes.iterator();

            boolean isPresent = false;

            System.out.println("Failed to ping the node " + node);

            //Remove the node from the K Bucket
            while (iterator.hasNext()) {
                final var currentNode = iterator.next();

                if (node.getNodeID().equals(currentNode.getNodeID())) {
                    isPresent = true;

                    iterator.remove();
                    break;
                }
            }

            if (isPresent) {
                //Add the latest seen node to the bucket if the node was present and was removed.
                popLatestSeenNodeInWaitingList(kBucketFor).ifPresent(bucketNodes::addLast);

                //Remove the work proof of this node as it is no longer online so it
                //Might have been poisoned in the mean time
                this.storedCRCs.remove(node.getNodeID());
            } else {
                //Remove the CRC request if the node is offline
                this.requestedCRCs.remove(node.getNodeID());
            }
        } finally {
            lock.unlock();
        }
    }

    public void handleSeenNode(NodeTriple seen) {

        final int kBucketFor = P2PStandards.getKBucketFor(this.nodeID, seen.getNodeID().getBytes());

        ReentrantLock lock = this.kBucketWriteLocks.get(kBucketFor);

        try {

            lock.lock();

            var nodeTriples = this.kBuckets.get(kBucketFor);

            if (nodeTriples == null) {
                nodeTriples = new ConcurrentLinkedDeque<>();
            }

            seen.setLastSeen(System.currentTimeMillis());

            System.out.println("Node " + seen + " belongs in bucket " + kBucketFor + " of " + Hex.toHexString(getNodeID()));

            final var iterator = nodeTriples.iterator();

            boolean alreadyPresent = false;

            while (iterator.hasNext()) {
                final var currentNode = iterator.next();

                if (seen.getNodeID().equals(currentNode.getNodeID())) {

                    alreadyPresent = true;

                    iterator.remove();

                    break;
                }
            }

            if (alreadyPresent) {
                System.out.println("Node was already present in bucket, moving it to the back of the list");

                //If the node was already present, move it to the tail of the list
                nodeTriples.addLast(seen);

                //Only remove the node after adding the previous, so we don't get any sort of concurrency
                //Issues
                //Removed because we are now using locks for each bucket
//            nodeTriples.removeFirstOccurrence(seen);

                //Discard of the oldest node in the waiting list, as the ping to the first one was successful
                popOldestSeenNodeInWaitingList(kBucketFor);
            } else {

                //The node is not already present, so we request it to perform a CRC
                //To accept this, send a challenge that requires computational work to the node to prevent sybil attacks
                //Maybe send a Large prime number N for it to factorize? Or maybe send him a random number and he has
                //To find a Proof of work with X zeroes to be able to join the network
                if (!hasSolvedCRC(seen)) {

                    System.out.println("Node has not solved CRC. Sending it.");
                    requestCRCFromNode(seen);

                    return;
                }

                if (nodeTriples.size() >= P2PStandards.K) {

                    //Ping the head of the list, wait for it's response.
                    //If it does not respond, remove it and concatenate this node into the last position of the array
                    //If it does respond, put it at the tail of the list and ignore this one
                    System.out.println("K bucket is full, appending to the wait list and pinging head");

                    appendWaitingNode(kBucketFor, seen);

                    pingHeadOfKBucket(kBucketFor);

                } else {
                    System.out.println("Added the node in the last position of the bucket.");
                    //The node is not present in the K bucket and the bucket is not empty, so add this node to the tail of the list
                    nodeTriples.addLast(seen);
                }
            }

            this.kBuckets.set(kBucketFor, nodeTriples);
        } finally {
            lock.unlock();
        }
    }

    public List<NodeTriple> findKClosestNodes(byte[] nodeID) {
        return findClosestNodes(P2PStandards.K, nodeID);
    }

    public void kBucketUpdated(int kBucket) {
        this.lastKBucketUpdate.set(kBucket, System.currentTimeMillis());
    }

    /**
     * Update the sorted set with the nodes from the given bucket
     *
     * @param maxNodeCount The node limit for the set (Usually {@link P2PStandards#K})
     * @param kBucket      The bucket to check
     * @param sortedNodes  The node set
     * @param lookupID     The ID of the center node
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

            BigInteger lastNodeDist = P2PStandards.nodeDistance(lastNode.getNodeID().getBytes(), lookupID),
                    nodeTripleDist = P2PStandards.nodeDistance(nodeTriple.getNodeID().getBytes(), lookupID);

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
     *
     * @param nodeCount The amount of nodes
     * @param nodeID    Node ID
     * @return
     */
    public List<NodeTriple> findClosestNodes(int nodeCount, byte[] nodeID) {

        int kBucketFor = P2PStandards.getKBucketFor(nodeID, this.getNodeID());

        TreeSet<NodeTriple> sortedNodes = new TreeSet<>(new KeyDistanceComparator(nodeID));

        //We start looking at the k bucket that the node ID should be contained in,
        //As that is the bucket that contains the nodes that are closest to it
        //Then we start expanding equally to the left and right buckets
        //Until we have filled the node set, or we have gone through all buckets.

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

    /**
     * Collect all of the nodes in our routing table
     * @return
     */
    public List<Pair<NodeTriple, Integer>> collectAllNodesInRoutingTable() {

        List<Pair<NodeTriple, Integer>> nodes = new LinkedList<>();

        for (int i = 0; i < this.kBuckets.size(); i++) {
            ConcurrentLinkedDeque<NodeTriple> kBucket = this.kBuckets.get(i);

            for (NodeTriple nodeTriple : kBucket) {
                nodes.add(Pair.of(nodeTriple, i));
            }
        }

        return nodes;
    }

    /**
     * Store a value in our node content
     * @param originNodeID
     * @param ID
     * @param value
     */
    public void storeValue(byte[] originNodeID, byte[] ID, byte[] value) {

        ByteWrapper wrapped_ID = new ByteWrapper(ID);

        if (Arrays.equals(originNodeID, this.getNodeID())) {
            this.publishedValues.put(wrapped_ID, new StoredKeyMetadata(ID, value, getNodeID()));
        }

        if (this.storedValues.containsKey(wrapped_ID)) {
            StoredKeyMetadata storedKey = this.storedValues.get(wrapped_ID);

            storedKey.setValue(value);
        } else {
            StoredKeyMetadata storedKey = new StoredKeyMetadata(ID, value, originNodeID);

            this.storedValues.put(wrapped_ID, storedKey);
        }
    }

    public byte[] loadValue(byte[] ID) {
        return this.storedValues.get(new ByteWrapper(ID)).getValue();
    }

    public void deleteValue(byte[] ID) {
        var wrappedID = new ByteWrapper(ID);

        this.publishedValues.remove(wrappedID);

        this.storedValues.remove(wrappedID);
    }

    public void registerRepublish() {
        this.lastRepublish = System.currentTimeMillis();
    }

    public void registerOriginalContentRepublish() {
        this.lastOriginalRepublish = System.currentTimeMillis();
    }

    public void registerNegativeInteraction(byte[] node) {
        this.interactionsPerPeer.compute(new ByteWrapper(node), (nodeID, current) -> {

            if (current == null) {
                return Pair.of(0, 1);
            }

            return current.mapValue((currentBad) -> currentBad + 1);
        });
    }

    public void registerPositiveInteraction(byte[] node) {
        this.interactionsPerPeer.compute(new ByteWrapper(node), (nodeID, current) -> {

            if (current == null) {
                return Pair.of(1, 0);
            }

            return current.mapKey((currentGood) -> currentGood + 1);
        });
    }

    public Pair<Integer, Integer> getRegisteredInteractions(byte[] nodeID) {
        return this.interactionsPerPeer.getOrDefault(new ByteWrapper(nodeID), Pair.of(0, 0));
    }

    /**
     * Register that an operation is ongoing
     * @param operation
     */
    public void registerOngoingOperation(Operation operation) {
        this.onGoingOperations.add(operation);
    }

    /**
     * Register that an operation is done
     * @param operation
     */
    public void registerOperationDone(Operation operation) {
        this.onGoingOperations.remove(operation);
    }

    public void waitForAllOperations() {
        while (!this.onGoingOperations.isEmpty()) { }
    }

    /**
     * Register that the node has seen a message
     * @param messageID
     * @return True if the message has never been seen, false if it has
     */
    public boolean registerSeenMessage(byte[] messageID) {
        return this.seenMessages.add(new ByteWrapper(messageID));
    }

    /**
     * Iterate to verify what operations must be performed
     * Namely stuff like refreshing buckets, republishing our stored values,
     * Content refresh, etc..
     */
    public void iterate() {

        for (int i = 0; i < this.kBuckets.size(); i++) {

            if (System.currentTimeMillis() - this.lastKBucketUpdate.get(i) > P2PStandards.T_REFRESH) {
                new RefreshBucketOperation(i, this).execute();
            }

        }

        for (Map.Entry<ByteWrapper, StoredKeyMetadata> storedData : this.storedValues.entrySet()) {

            if (System.currentTimeMillis() - storedData.getValue().getLastUpdated() > P2PStandards.T_EXPIRE) {
                deleteValue(storedData.getKey().getBytes());
            }

        }

        if (System.currentTimeMillis() - lastOriginalRepublish > P2PStandards.T_REPUBLISH) {
            registerOriginalContentRepublish();

            new OriginalContentRepublishOperation(this).execute();
        }

        if (System.currentTimeMillis() - lastRepublish > P2PStandards.T_REPLICATE) {
            registerRepublish();

            new ContentRepublishOperation(this).execute();
        }

    }

}
