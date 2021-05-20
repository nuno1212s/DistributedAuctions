package me.evmanu.p2p.grpc;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import me.evmanu.FoundNode;
import me.evmanu.P2PServerGrpc;
import me.evmanu.Ping;
import me.evmanu.TargetID;
import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.operations.NodeLookup;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DistLedgerClientManager {

    private static DistLedgerClientManager instance;

    public static DistLedgerClientManager getInstance() {
        return instance;
    }

    private final Map<byte[], ManagedChannel> cachedChannels;

    public DistLedgerClientManager() {
        instance = this;

        this.cachedChannels = new HashMap<>();
    }

    private ManagedChannel getCachedChannel(byte[] nodeID) {
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
        ManagedChannel managedChannel = this.cachedChannels.remove(nodeID);

        if (managedChannel.isTerminated() || managedChannel.isShutdown()) {

            return;
        }

        managedChannel.shutdown();
    }

    private ManagedChannel setupConnection(NodeTriple nodeTriple) throws IOException {
        final var cachedChannel = getCachedChannel(nodeTriple.getNodeID());

        if (cachedChannel == null) {
            final var createdConnection = createConnection(nodeTriple);

            cacheConnection(nodeTriple, createdConnection);

            return createdConnection;
        }

        throw new IOException("Failed to connect to the IP: " + nodeTriple.getIpAddress().getHostAddress());
    }

    public P2PServerGrpc.P2PServerStub newStub(NodeTriple triple) {
        try {
            final var connection = setupConnection(triple);

            return P2PServerGrpc.newStub(connection);
        } catch (IOException e) {
            e.printStackTrace();

            System.out.println("Failed to connect.");
        }

        return null;
    }

    public P2PServerGrpc.P2PServerFutureStub newFutureStub(NodeTriple triple) throws IOException {
        final var connection = setupConnection(triple);

        return P2PServerGrpc.newFutureStub(connection);
    }

    public void performPingFor(P2PNode node, NodeTriple triple) {
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
    }

    public void performLookupFor(P2PNode node, NodeLookup operation, NodeTriple target, byte[] lookup) {

        P2PServerGrpc.P2PServerStub p2PServerStub = this.newStub(target);

        List<NodeTriple> nodeTripleList = new LinkedList<>();

        p2PServerStub.findNode(TargetID.newBuilder().setTargetID(ByteString.copyFrom(lookup)).build(), new StreamObserver<>() {
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

    }

}
