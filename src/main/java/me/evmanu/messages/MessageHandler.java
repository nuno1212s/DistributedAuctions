package me.evmanu.messages;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.evmanu.Standards;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.blocks.BlockChain;
import me.evmanu.blockchain.blocks.BlockChainHandler;
import me.evmanu.messages.adapters.BlockAdapter;
import me.evmanu.messages.adapters.MessageAdapter;
import me.evmanu.messages.messagetypes.*;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.nodeoperations.BroadcastMessageOperation;
import me.evmanu.p2p.nodeoperations.SendMessageOperation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

public class MessageHandler {

    private final P2PNode node;

    private final BlockChainHandler blockChainHandler;

    private final Gson gson;

    public MessageHandler(P2PNode node, BlockChainHandler blockChainHandler) {
        this.node = node;
        this.blockChainHandler = blockChainHandler;

        this.gson = new GsonBuilder()
                .registerTypeAdapter(Message.class, new MessageAdapter())
                .registerTypeAdapter(Block.class, new BlockAdapter())
                .create();
    }

    public void publishMessage(Message message) {
        String json = gson.toJson(message);

        MessageDigest digest = Standards.getDigestInstance();

        digest.update(json.getBytes(StandardCharsets.UTF_8));

        byte[] hash = digest.digest();

        new BroadcastMessageOperation(node, 0, hash, json.getBytes(StandardCharsets.UTF_8))
                .execute();
    }

    public void receiveMessageFrom(byte[] nodeID, byte[] message) {

        String json = new String(message, StandardCharsets.UTF_8);

        Message parsedMessage = gson.fromJson(json, Message.class);

        switch (parsedMessage.getType()) {

            case BROADCAST_TRANSACTION: {
                TransactionMessage transactions = (TransactionMessage) parsedMessage;

                blockChainHandler.getTransactionPool().receiveTransaction(transactions.getTransaction());
                break;
            }

            case BROADCAST_BLOCK: {
                BlockMessage blockMessage = (BlockMessage) parsedMessage;

                blockChainHandler.addBlockToChainAndUpdate(blockMessage.getBlock());
                break;
            }

            case REQUEST_BLOCK_CHAIN: {
                sendBlockChainInfoTo(nodeID);
                break;
            }

            case REQUEST_BLOCK: {

                RequestBlockMessage requestBlockMessage = (RequestBlockMessage) parsedMessage;

                sendBlockInfo(nodeID, requestBlockMessage.getBlockID(), requestBlockMessage.getPreviousBlockHash());

                break;
            }

            case BLOCK_CHAIN_INFO: {

            }
        }
    }

    public void sendBlockChainInfoTo(byte[] nodeID) {
        Optional<BlockChain> bestCurrentChain = blockChainHandler.getBestCurrentChain();

        if (bestCurrentChain.isPresent()) {

            BlockChainInfoMessage currentChain = new BlockChainInfoMessage(bestCurrentChain.get().getBlockCount());

            byte[] message = gson.toJson(currentChain).getBytes(StandardCharsets.UTF_8);

            new SendMessageOperation(node, nodeID, message).execute();
        }
    }

    public void sendBlockInfo(byte[] nodeID, long blockNum, byte[] prevBlockHash) {

        Optional<Block> block = blockChainHandler.getBlockByPreviousHashAndBlockNumber(blockNum, prevBlockHash);

        if (block.isPresent()) {
            BlockMessage blockMessage = new BlockMessage(block.get());

            byte[] message = gson.toJson(blockMessage).getBytes(StandardCharsets.UTF_8);

            new SendMessageOperation(node, nodeID, message).execute();
        }
    }

}
