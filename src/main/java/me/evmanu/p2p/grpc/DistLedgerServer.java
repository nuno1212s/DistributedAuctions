package me.evmanu.p2p.grpc;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import me.evmanu.Ping;
import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.util.Hex;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DistLedgerServer {

    private static final int PORT = 8080;

    private static final Logger logger = Logger.getLogger(DistLedgerServer.class.getName());

    private Server server;

    private final DistLedgerClientManager clientManager;

    private DistLedgerServerImpl serverImpl;

    public DistLedgerServer() {
        this.clientManager = new DistLedgerClientManager();
    }

    public P2PNode start() throws IOException {
        //TODO: Generate a node ID

        final var p2PNode = new P2PNode(Hex.fromHexString("0123456789abcdef0123"), clientManager);

        serverImpl = new DistLedgerServerImpl(logger, p2PNode);

        server = ServerBuilder.forPort(PORT)
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

    public static void main(String[] args) {

        DistLedgerServer server = new DistLedgerServer();


        try {
            final var node = server.start();

            Thread thread = new Thread(() -> {

                try {
                    NodeTriple triple = new NodeTriple(InetAddress.getLocalHost(), PORT, node.getNodeID(),
                            System.currentTimeMillis());

                    final var p2PServerStub = server.clientManager.newStub(triple);

                    p2PServerStub.ping(Ping.newBuilder().setNodeID(ByteString.copyFrom(node.getNodeID())).build(), new StreamObserver<Ping>() {
                        @Override
                        public void onNext(Ping value) {
                            System.out.println("LOL");
                        }

                        @Override
                        public void onError(Throwable t) {
                        }

                        @Override
                        public void onCompleted() {

                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            System.out.println("Started server.");

            thread.start();

            server.blockUntilShutdown();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

}
