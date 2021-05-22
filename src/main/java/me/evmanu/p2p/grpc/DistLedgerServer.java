package me.evmanu.p2p.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.P2PStandards;
import me.evmanu.util.Hex;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DistLedgerServer {

    public static final int DEFAULT_PORT = 80;

    private static final Logger logger = Logger.getLogger(DistLedgerServer.class.getName());

    private static final Random random = new Random();

    private Server server;

    private final DistLedgerClientManager clientManager;

    private DistLedgerServerImpl serverImpl;

    public DistLedgerServer() {
        this.clientManager = new DistLedgerClientManager();
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

    public static void main(String[] args) {

        DistLedgerServer server = new DistLedgerServer();

        Options options = new Options();

        Option shouldBoostrap = new Option("bst", "boostrap", false, "Make the node boostrap");

        shouldBoostrap.setRequired(false);

        Option nodeID = new Option("nid", "nodeid", true, "Manually select a node ID");

        shouldBoostrap.setRequired(false);

        Option portArg = new Option("port", "port", true, "Manually select a port");

        portArg.setRequired(false);

        options.addOption(nodeID);

        options.addOption(portArg);

        options.addOption(shouldBoostrap);

        CommandLineParser cmdLineParser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = cmdLineParser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Kademlia", options);

            System.exit(1);
            return;
        }

        try {

            String indicatedNodeID = null;

            if (cmd.hasOption("nid")) {
                indicatedNodeID = cmd.getOptionValue("nid");
            }

            int port = DEFAULT_PORT;

            if (cmd.hasOption("port")) {
                port = Integer.parseInt(cmd.getOptionValue("port"));
            }

            final var node = server.start(indicatedNodeID, port);

            Thread thread = null;
            if (cmd.hasOption("bst")) {
                thread = new Thread(() -> {
                    node.boostrap(P2PStandards.getBOOSTRAP_NODES());
                });

                thread.start();
            }

            System.out.println("Started server.");

            server.blockUntilShutdown();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

}
