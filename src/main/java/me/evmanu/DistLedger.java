package me.evmanu;

import lombok.Setter;
import me.evmanu.blockchain.BlockChainHandler;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.blocks.BlockChain;
import me.evmanu.blockchain.blocks.blockchains.PoWBlockChain;
import me.evmanu.messages.MessageHandler;
import me.evmanu.messages.messagetypes.BlockChainRequestMessage;
import me.evmanu.p2p.grpc.DistLedgerServer;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.P2PStandards;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Setter
public class DistLedger {

    private static final ScheduledExecutorService executors = Executors.newScheduledThreadPool(1);

    public static final int DEFAULT_PORT = 80;

    private P2PNode node;

    private DistLedgerServer ledgerServer;

    private MessageHandler messageHandler;

    private BlockChainHandler chainHandler;

    private Thread serverBlockedThread;

    public DistLedger(String[] args) {

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

        startP2PServer(cmd);

        initializeBlockChain();

        initMessageHandler();

        executors.schedule(this::requestBlockChain, 4, TimeUnit.SECONDS);
    }

    private void initMessageHandler() {
        this.messageHandler = new MessageHandler(node, this.chainHandler);

        this.ledgerServer.registerMessageListener(messageHandler::receiveMessageFrom);

        System.out.println("Initialized messages...");
    }

    private void startP2PServer(CommandLine cmd) {

        this.ledgerServer = new DistLedgerServer();

        try {

            String indicatedNodeID = null;

            if (cmd.hasOption("nid")) {
                indicatedNodeID = cmd.getOptionValue("nid");
            }

            int port = DEFAULT_PORT;

            if (cmd.hasOption("port")) {
                port = Integer.parseInt(cmd.getOptionValue("port"));
            }

            this.node = this.ledgerServer.start(indicatedNodeID, port);

            Thread thread = null;

            if (cmd.hasOption("bst")) {
                thread = new Thread(() -> node.boostrap(P2PStandards.getBOOSTRAP_NODES()));

                thread.start();
            }

            System.out.println("Started server.");

            this.serverBlockedThread = new Thread(() -> {
                try {
                    this.ledgerServer.blockUntilShutdown();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            this.serverBlockedThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeBlockChain() {
        //TODO
        this.chainHandler = new BlockChainHandler(new PoWBlockChain(0, (short) 0x1, new ArrayList<>()));

        System.out.println("Initialized block chain");
    }

    private void requestBlockChain() {

        try {
            System.out.println("Requesting block chain...");

            var bestCurrentChain = this.chainHandler.getBestCurrentChain();

            if (bestCurrentChain.isPresent()) {
                if (bestCurrentChain.get().getBlockCount() == 0) {
                    System.out.println("Publishing with 0");
                    this.messageHandler.publishMessage(new BlockChainRequestMessage(0, new byte[0]));
                } else {

                    var latestValidBlock = bestCurrentChain.get().getLatestValidBlock();

                    var rqMessage = new BlockChainRequestMessage(latestValidBlock.getHeader().getBlockNumber(),
                            latestValidBlock.getHeader().getBlockHash());

                    System.out.println("Publishing with " + latestValidBlock.getHeader().getBlockNumber());

                    this.messageHandler.publishMessage(rqMessage);
                }
            } else {
                System.out.println("No current chain?");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new DistLedger(args);
    }
}
