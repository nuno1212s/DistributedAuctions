package me.evmanu.p2p;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import me.evmanu.P2PServerGrpc;
import me.evmanu.p2p.kademlia.NodeTriple;

import java.io.IOException;
import java.util.HashMap;
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
                .build();

    }

    private void cacheConnection(NodeTriple triple, ManagedChannel channel) {
        this.cachedChannels.put(triple.getNodeID(), channel);
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

    public P2PServerGrpc.P2PServerStub newStub(NodeTriple triple) throws IOException {
        final var connection = setupConnection(triple);

        return P2PServerGrpc.newStub(connection);
    }

    public P2PServerGrpc.P2PServerFutureStub newFutureStub(NodeTriple triple) throws IOException {
        final var connection = setupConnection(triple);

        return P2PServerGrpc.newFutureStub(connection);
    }



}
