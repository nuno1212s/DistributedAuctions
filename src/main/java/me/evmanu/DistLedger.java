package me.evmanu;

import lombok.Setter;
import me.evmanu.blockchain.BlockChainHandler;
import me.evmanu.blockchain.blocks.blockchains.PoWBlockChain;
import me.evmanu.blockchain.transactions.ScriptPubKey;
import me.evmanu.blockchain.transactions.ScriptSignature;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.blockchain.transactions.TransactionType;
import me.evmanu.messages.MessageHandler;
import me.evmanu.messages.messagetypes.BlockChainRequestMessage;
import me.evmanu.messages.messagetypes.BlockMessage;
import me.evmanu.messages.messagetypes.TransactionMessage;
import me.evmanu.miner.MiningManager;
import me.evmanu.p2p.grpc.DistLedgerServer;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.P2PStandards;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.security.KeyPair;
import java.util.ArrayList;
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

    private MiningManager miningManager;

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

        this.miningManager.registerMinedBlockListener((block) -> {
            System.out.println("PUBLISHING A NEW BLOCK.");

            messageHandler.publishMessage(new BlockMessage(block));
        });

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

        this.miningManager = new MiningManager(this.chainHandler.getTransactionPool(), this.chainHandler);

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

        testPublishTransaction();
    }

    private void testPublishTransaction() {

        var keyPair = Standards.getKeyGenerator().generateKeyPair();

        var transaction = initGenesisTransactionFor(10, keyPair);

        System.out.println("Publish transaction " + transaction);

        this.messageHandler.publishMessage(new TransactionMessage(transaction));

        this.chainHandler.getTransactionPool().receiveTransaction(transaction);

    }

    public static Transaction initGenesisTransactionFor(float amountPerOutput, KeyPair... outputs) {

        final var keyGenerator = Standards.getKeyGenerator();

        assert keyGenerator != null;

        var output = new ScriptPubKey[outputs.length];

        for (int i = 0; i < outputs.length; i++) {
            var outputI = outputs[i];

            output[i] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(outputI.getPublic()), amountPerOutput);
        }

        return new Transaction((short) 0x1, TransactionType.TRANSACTION, new ScriptSignature[0], output);
    }


    public static void main(String[] args) {
        new DistLedger(args);
    }
}
