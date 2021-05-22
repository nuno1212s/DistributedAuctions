package me.evmanu.p2p.grpc;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import me.evmanu.*;
import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.StoredKeyMetadata;
import me.evmanu.p2p.nodeoperations.*;
import me.evmanu.util.ByteWrapper;
import me.evmanu.util.Pair;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class DistLedgerClientManager {

    private static DistLedgerClientManager instance;

    public static DistLedgerClientManager getInstance() {
        return instance;
    }

    private final Map<ByteWrapper, ManagedChannel> cachedChannels;

    public DistLedgerClientManager() {
        instance = this;

        this.cachedChannels = new ConcurrentHashMap<>();
    }

    private ManagedChannel getCachedChannel(ByteWrapper nodeID) {

        if (cachedChannels.containsKey(nodeID)) {
            final var managedChannel = this.cachedChannels.get(nodeID);

            if (managedChannel.isTerminated() || managedChannel.isTerminated()) {
                return null;
            }

            return managedChannel;
        }

        return null;
    }

    private ManagedChannel createConnection(NodeTriple triple) {

        return ManagedChannelBuilder
                .forAddress(triple.getIpAddress().getHostAddress(), triple.getUdpPort())
                .usePlaintext()
                .build();

    }

    private void cacheConnection(NodeTriple triple, ManagedChannel channel) {
        this.cachedChannels.put(triple.getNodeID(), channel);
    }

    private void closeConnection(byte[] nodeID) {
        ManagedChannel managedChannel = this.cachedChannels.remove(new ByteWrapper(nodeID));

        if (managedChannel.isTerminated() || managedChannel.isShutdown()) {

            return;
        }

        managedChannel.shutdown();
    }

    private ManagedChannel setupConnection(NodeTriple nodeTriple) throws IOException {
        final var cachedChannel = getCachedChannel(nodeTriple.getNodeID());

        if (cachedChannel == null || cachedChannel.isShutdown() || cachedChannel.isTerminated()) {
            final var createdConnection = createConnection(nodeTriple);

            cacheConnection(nodeTriple, createdConnection);

            return createdConnection;
        }

        throw new IOException("Failed to connect to the IP: " + nodeTriple.getIpAddress().getHostAddress());
    }

    public P2PServerGrpc.P2PServerStub newStub(NodeTriple triple) throws IOException {
        final var connection = setupConnection(triple);

        return P2PServerGrpc.newStub(connection);
    }

    public P2PServerGrpc.P2PServerFutureStub newFutureStub(NodeTriple triple) throws IOException {
        final var connection = setupConnection(triple);

        return P2PServerGrpc.newFutureStub(connection);
    }

    public void performPingFor(P2PNode node, NodeTriple triple) {
        try {
            P2PServerGrpc.P2PServerStub tripleStub = this.newStub(triple);

            tripleStub.ping(Ping.newBuilder().setNodeID(ByteString.copyFrom(node.getNodeID())).build(),
                    new StreamObserver<>() {
                        @Override
                        public void onNext(Ping value) {
                            node.handleSeenNode(triple);
                        }

                        @Override
                        public void onError(Throwable t) {
                            node.handleFailedNodePing(triple);
                        }

                        @Override
                        public void onCompleted() {
                        }
                    });
        } catch (IOException e) {
            node.handleFailedNodePing(triple);
        }

    }

    public void performLookupFor(P2PNode node, NodeLookupOperation operation, NodeTriple target, byte[] lookup) {

        try {
            P2PServerGrpc.P2PServerStub p2PServerStub = this.newStub(target);

            List<NodeTriple> nodeTripleList = new LinkedList<>();

            TargetID targetID = TargetID.newBuilder()
                    .setRequestingNodeID(ByteString.copyFrom(node.getNodeID()))
                    .setRequestNodePort(node.getNodePublicPort())
                    .setTargetID(ByteString.copyFrom(lookup)).build();

            p2PServerStub.findNode(targetID, new StreamObserver<>() {
                @Override
                public void onNext(FoundNode value) {
                    byte[] nodeID = value.getNodeID().toByteArray();

                    String nodeAddress = value.getNodeAddress();

                    int port = value.getPort();

                    try {
                        InetAddress ipAddress = InetAddress.getByName(nodeAddress);

                        nodeTripleList.add(new NodeTriple(ipAddress, port, nodeID, value.getLastSeen()));
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    //We will pass it on the onCompleted method, to make sure we have already received all of them.
                }

                @Override
                public void onError(Throwable t) {
                    operation.handleFindNodeFailed(target);
                }

                @Override
                public void onCompleted() {
                    operation.handleFindNodeReturned(target, nodeTripleList);
                }
            });
        } catch (IOException e) {
            operation.handleFindNodeFailed(target);
        }
    }

    public void performStoreFor(P2PNode node, StoreOperationBase storeOperation,
                                NodeTriple destination, StoredKeyMetadata metadata) {

        P2PServerGrpc.P2PServerStub destinationStub = null;

        try {
            destinationStub = this.newStub(destination);
        } catch (IOException e) {
            storeOperation.handleFailedStore(destination);

            return;
        }

        Store msg = Store.newBuilder().setRequestingNodeID(ByteString.copyFrom(node.getNodeID()))
                .setRequestingNodePort(node.getNodePublicPort())
                .setKey(ByteString.copyFrom(metadata.getKey()))
                .setValue(ByteString.copyFrom(metadata.getValue()))
                .setOwningNodeID(ByteString.copyFrom(metadata.getOwnerNodeID()))
                .build();

        destinationStub.store(msg, new StreamObserver<>() {
            @Override
            public void onNext(Store value) {
            }

            @Override
            public void onError(Throwable t) {
                storeOperation.handleFailedStore(destination);
            }

            @Override
            public void onCompleted() {
                storeOperation.handleSuccessfulStore(destination);
            }
        });

    }

    public void requestCRCFromNode(P2PNode sender, NodeTriple destination, long challenge) {

        P2PServerGrpc.P2PServerStub destinationStub = null;
        try {
            destinationStub = this.newStub(destination);
        } catch (IOException e) {

            e.printStackTrace();

            sender.handleFailedNodePing(destination);

            return;
        }

        CRCRequest crrequest = CRCRequest.newBuilder()
                .setChallengingNodeID(ByteString.copyFrom(sender.getNodeID()))
                .setChallengingNodePort(sender.getNodePublicPort())
                .setChallenge(challenge)
                .build();

        destinationStub.requestCRC(crrequest, new StreamObserver<>() {
            @Override
            public void onNext(CRCResponse value) {
                sender.receivedCRCFromNode(destination, Pair.of(value.getChallenge(), value.getResponse()));
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                sender.handleFailedNodePing(destination);
            }

            @Override
            public void onCompleted() {
            }
        });

    }

    public void findValueFromNode(P2PNode node, ContentLookupOperation lookupOperation, NodeTriple destination, byte[] target) {

        try {
            P2PServerGrpc.P2PServerStub destinationStub = newStub(destination);

            TargetID targetID = TargetID.newBuilder()
                    .setRequestingNodeID(ByteString.copyFrom(node.getNodeID()))
                    .setRequestNodePort(node.getNodePublicPort())
                    .setTargetID(ByteString.copyFrom(target)).build();

            AtomicReference<byte[]> foundValue = new AtomicReference<>(null);

            List<NodeTriple> foundNodes = new LinkedList<>();

            destinationStub.findValue(targetID, new StreamObserver<>() {
                @Override
                public void onNext(FoundValue value) {
                    if (value.getValueKind() == StoreKind.VALUE_FOUND) {
                        foundValue.set(value.getValue().toByteArray());
                    } else {
                        byte[] nodeID = value.getNodeID().toByteArray();

                        int port = value.getPort();

                        try {
                            InetAddress inetAddress = InetAddress.getByName(value.getNodeAdress());

                            foundNodes.add(new NodeTriple(inetAddress, port, nodeID, System.currentTimeMillis()));

                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onError(Throwable t) {
                    lookupOperation.nodeFailedLookup(destination);
                }

                @Override
                public void onCompleted() {
                    if (foundValue.get() == null) {
                        lookupOperation.nodeReturnedMoreNodes(destination, foundNodes);
                    } else {
                        lookupOperation.handleFoundValue(destination, foundValue.get());
                    }
                }
            });
        } catch (IOException e) {
            lookupOperation.nodeFailedLookup(destination);
        }

    }

}
