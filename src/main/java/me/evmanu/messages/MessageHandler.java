package me.evmanu.messages;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.evmanu.Standards;
import me.evmanu.auctions.AuctionHandler;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.blocks.BlockChain;
import me.evmanu.blockchain.BlockChainHandler;
import me.evmanu.blockchain.blocks.blockchains.PoSBlock;
import me.evmanu.blockchain.blocks.blockchains.PoWBlock;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.messages.adapters.BlockAdapter;
import me.evmanu.messages.adapters.MessageAdapter;
import me.evmanu.messages.messagetypes.*;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.nodeoperations.BroadcastMessageOperation;
import me.evmanu.p2p.nodeoperations.SendMessageOperation;
import me.evmanu.util.Hex;

import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.function.Consumer;

public class MessageHandler {

    private final P2PNode node;

    private final BlockChainHandler blockChainHandler;

    private final AuctionHandler auctionHandler;

    private final Gson gson;

    public MessageHandler(P2PNode node, BlockChainHandler blockChainHandler, AuctionHandler auctionHandler) {
        this.node = node;
        this.blockChainHandler = blockChainHandler;
        this.auctionHandler = auctionHandler;

        var gsonBuilder = new GsonBuilder();

        var messageAdapter = new MessageAdapter();
        var blockAdapter = new BlockAdapter();

        this.gson = gsonBuilder
                .registerTypeAdapter(Message.class, messageAdapter)
                .registerTypeAdapter(Block.class, blockAdapter)
                .excludeFieldsWithModifiers(Modifier.TRANSIENT)
                .create();
    }

    /**
     * Broadcasts the message into the p2p network
     *
     * @param message
     */
    public void publishMessage(MessageContent message) {
        String json = null;

        Message finalMsg = new Message(message);

        json = gson.toJson(finalMsg);

        System.out.println("Broadcasting " + json);

        MessageDigest digest = Standards.getDigestInstance();

        digest.update(json.getBytes(StandardCharsets.UTF_8));

        var buffer = ByteBuffer.allocate(Long.BYTES);

        digest.update(buffer.array());

        byte[] hash = digest.digest();

        new BroadcastMessageOperation(node, 0, hash, json.getBytes(StandardCharsets.UTF_8))
                .execute();
    }

    public byte[] receiveMessageFrom(byte[] nodeID, byte[] message) {

        String json = new String(message, StandardCharsets.UTF_8);

        System.out.println("Parsing message " + json);

        Message parsedMessage = gson.fromJson(json, Message.class);

        System.out.println("Received message of the type " + parsedMessage.getContent().getType().name());

        switch (parsedMessage.getContent().getType()) {

            case BROADCAST_TRANSACTION: {

                TransactionMessage transactions = (TransactionMessage) parsedMessage.getContent();

                var transaction = transactions.getTransaction();

                System.out.println("Received a transaction " + transaction);

                if (!blockChainHandler.getTransactionPool().receiveTransaction(transaction)) {
                    System.out.println("FAILED TO ADD TRANSACTION");

                    publishMessage(new TransactionRejectMessage(transaction.getTxID()));
                }

                break;
            }

            case BROADCAST_BLOCK: {
                BlockMessage blockMessage = (BlockMessage) parsedMessage.getContent();

                System.out.println("Received new block " + blockMessage.getBlock().getHeader().getBlockNumber() +
                        " with hash " +
                        Hex.toHexString(blockMessage.getBlock().getHeader().getBlockHash()));

                if (!blockChainHandler.addBlockToChainAndUpdate(blockMessage.getBlock())) {

                    var header = blockMessage.getBlock().getHeader();

                    publishMessage(new BlockRejectMessage(nodeID, header.getBlockNumber(),
                            header.getBlockHash()));
                } else {

                    System.out.println("Received and added new block " + blockMessage.getBlock().getHeader().getBlockNumber() +
                            " with hash " +
                            Hex.toHexString(blockMessage.getBlock().getHeader().getBlockHash()));
                }
                break;
            }

            case REQUEST_BLOCK: {

                RequestBlockMessage requestBlockMessage = (RequestBlockMessage) parsedMessage.getContent();

                sendBlockInfo(nodeID, requestBlockMessage.getBlockID(), requestBlockMessage.getPreviousBlockHash());

                break;
            }

            case REQUEST_BLOCK_CHAIN: {

                BlockChainRequestMessage requestMessage = (BlockChainRequestMessage) parsedMessage.getContent();

                blockChainHandler.getBestCurrentChain().ifPresent(chain ->
                {
                    if (chain.getBlockCount() <= requestMessage.getCurrentBlock()) {
                        return;
                    }

                    for (long cur = requestMessage.getCurrentBlock(); cur < chain.getBlockCount(); cur++) {
                        sendBlock(nodeID, chain.getBlockByNumber(cur));
                    }
                });

                break;
            }

            case BLOCK_CHAIN_INFO: {
                BlockChainInfoMessage info = (BlockChainInfoMessage) parsedMessage.getContent();

                System.out.println("Received block chain info: " + info.getBlockCount() + " blocks");
                break;
            }
            case AUCTION_ANNOUNCE: {

                AuctionMessage auctionMessage = (AuctionMessage) parsedMessage.getContent();

                auctionHandler.handleNewAuctionStarted(auctionMessage.getAuctionID());

                break;
            }
            case REQUEST_PAYMENT: {

                RequestPaymentMessage paymentMessage = (RequestPaymentMessage) parsedMessage.getContent();

                auctionHandler.handlePaymentRequest(paymentMessage.getBidID(),
                        paymentMessage.getAuctionID(), paymentMessage.getDestinationPubKeyHash());

                break;
            }
            case BID_ANNOUNCE: {

                BidMessage bidMessage = (BidMessage) parsedMessage.getContent();

                auctionHandler.handleNewBid(bidMessage.getBidID());

                break;
            }
            case AUCTION_REQUEST: {

                auctionHandler.handleRequestActiveAuctions(nodeID);

                break;
            }

        }

        return new byte[0];
    }

    public void sendBlockChainInfoTo(byte[] nodeID) {
        Optional<BlockChain> bestCurrentChain = blockChainHandler.getBestCurrentChain();

        if (bestCurrentChain.isPresent()) {

            BlockChainInfoMessage currentChain = new BlockChainInfoMessage(bestCurrentChain.get().getBlockCount());

            Message msg = new Message(currentChain);

            byte[] message = gson.toJson(msg).getBytes(StandardCharsets.UTF_8);

            new SendMessageOperation(node, nodeID, message).execute();
        }
    }

    public void sendBlock(byte[] nodeID, Block block) {

        var blockMsg = new BlockMessage(block);

        Message msg = new Message(blockMsg);

        byte[] serializedMsg = gson.toJson(msg).getBytes(StandardCharsets.UTF_8);

        new SendMessageOperation(node, nodeID, serializedMsg).execute();
    }

    public void sendBlockInfo(byte[] nodeID, long blockNum, byte[] prevBlockHash) {

        Optional<Block> block = blockChainHandler.getBlockByPreviousHashAndBlockNumber(blockNum, prevBlockHash);

        if (block.isPresent()) {
            BlockMessage blockMessage = new BlockMessage(block.get());

            Message msg = new Message(blockMessage);

            byte[] message = gson.toJson(msg).getBytes(StandardCharsets.UTF_8);

            new SendMessageOperation(node, nodeID, message).execute();
        }
    }


    public void sendMessage(byte[] nodeID, MessageContent message, Consumer<byte[]> response, Runnable failed) {

        Message msgObj = new Message(message);

        var serializedMsg = gson.toJson(msgObj).getBytes(StandardCharsets.UTF_8);

        var sendMsg = new SendMessageOperation(node, nodeID, serializedMsg);
        sendMsg.setFailedConsumer(failed);

        sendMsg.setResponseConsumer(response);

        sendMsg.execute();
    }

    public void sendMessage(byte[] nodeID, MessageContent message) {

        Message msgObj = new Message(message);

        var serializedMsg = gson.toJson(msgObj).getBytes(StandardCharsets.UTF_8);

        new SendMessageOperation(node, nodeID, serializedMsg).execute();
    }

    public <T> T deserializeObject(byte[] json, Class<T> toDeserialize) {
        var jsonStr = new String(json, StandardCharsets.UTF_8);

        return gson.fromJson(jsonStr, toDeserialize);
    }

    public byte[] serializeObject(Object obj) {
        return gson.toJson(obj).getBytes(StandardCharsets.UTF_8);
    }

}
