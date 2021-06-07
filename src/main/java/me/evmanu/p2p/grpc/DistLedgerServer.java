package me.evmanu.p2p.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.P2PStandards;
import me.evmanu.util.Hex;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public class DistLedgerServer {

    private static final Logger logger = Logger.getLogger(DistLedgerServer.class.getName());

    private static final Random random = new Random();

    private Server server;

    private final DistLedgerClientManager clientManager;

    private DistLedgerServerImpl serverImpl;

    public DistLedgerServer() {
        this.clientManager = new DistLedgerClientManager();
    }

    public void registerMessageListener(BiConsumer<byte[], byte[]> messageConsumer) {
        serverImpl.registerConsumer(messageConsumer);
    }

    public P2PNode start(String nodeHexString, int port) throws IOException {
        byte[] nodeID = null;

        if (nodeHexString != null) {
            nodeID = Hex.fromHexString(nodeHexString);
        } else {
            nodeID = new byte[P2PStandards.I / Byte.SIZE];

            random.nextBytes(nodeID);
        }

        final var p2PNode = new P2PNode(nodeID, clientManager, port);

        System.out.println("Initialized the node with an ID: " + Hex.toHexString(nodeID));

        serverImpl = new DistLedgerServerImpl(logger, p2PNode);

        server = ServerBuilder.forPort(port)
                .intercept(new ConnInterceptor(p2PNode))
                .addService(serverImpl)
                .build().start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                DistLedgerServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }

            System.err.println("*** server shut down");
        }));

        return p2PNode;
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(5, TimeUnit.MINUTES);
        }
    }

}
