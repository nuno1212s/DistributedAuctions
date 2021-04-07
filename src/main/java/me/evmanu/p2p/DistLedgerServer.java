package me.evmanu.p2p;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DistLedgerServer {

    private static int PORT = 8080;

    private static final Logger logger = Logger.getLogger(DistLedgerServer.class.getName());

    private Server server;

    private DistLedgerServerImpl serverImpl;

    public void start() throws IOException {
        serverImpl = new DistLedgerServerImpl(logger);

        server = ServerBuilder.forPort(PORT)
                .addService(serverImpl)
                .build().start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    DistLedgerServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
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
            server.start();

            server.blockUntilShutdown();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

}
