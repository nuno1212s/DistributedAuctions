package me.evmanu;

import lombok.Setter;
import me.evmanu.blockchain.blocks.BlockChainHandler;
import me.evmanu.messages.MessageHandler;
import me.evmanu.p2p.grpc.DistLedgerServer;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.P2PStandards;
import org.apache.commons.cli.*;

import java.io.IOException;

@Setter
public class DistLedger {

    public static final int DEFAULT_PORT = 80;

    private P2PNode node;

    private DistLedgerServer ledgerServer;

    private MessageHandler messageHandler;

    private BlockChainHandler chainHandler;

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

        //initializeBlockChain();

        //initMessageHandler();
    }

    private void initMessageHandler() {
        this.messageHandler = new MessageHandler(node, this.chainHandler);

        this.ledgerServer.registerMessageListener(messageHandler::receiveMessageFrom);
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

            this.ledgerServer.blockUntilShutdown();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void initializeBlockChain() {
        this.chainHandler = new BlockChainHandler();
    }

    public static void main(String[] args) {
        new DistLedger(args);
    }
}
