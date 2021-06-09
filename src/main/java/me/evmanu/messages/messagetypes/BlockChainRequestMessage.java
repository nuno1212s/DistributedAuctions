package me.evmanu.messages.messagetypes;

import lombok.Getter;
import me.evmanu.messages.Message;
import me.evmanu.messages.MessageContent;
import me.evmanu.messages.MessageType;

@Getter
public class BlockChainRequestMessage extends MessageContent {

    private final long currentBlock;

    private final byte[] blockHash;

    public BlockChainRequestMessage(long currentBlock,
                                    byte[] blockHash) {
        this.currentBlock = currentBlock;
        this.blockHash = blockHash;
    }

    @Override
    public MessageType getType() {
        return MessageType.REQUEST_BLOCK_CHAIN;
    }
}
