package me.evmanu.messages.messagetypes;

import lombok.Getter;
import me.evmanu.messages.Message;
import me.evmanu.messages.MessageContent;
import me.evmanu.messages.MessageType;

@Getter
public class BlockChainInfoMessage extends MessageContent {

    private final long blockCount;

    public BlockChainInfoMessage(long blockCount) {
        this.blockCount = blockCount;
    }

    @Override
    public MessageType getType() {
        return MessageType.BLOCK_CHAIN_INFO;
    }
}
