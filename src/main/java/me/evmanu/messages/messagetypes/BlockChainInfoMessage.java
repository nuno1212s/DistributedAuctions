package me.evmanu.messages.messagetypes;

import lombok.Getter;
import me.evmanu.messages.Message;
import me.evmanu.messages.MessageType;

@Getter
public class BlockChainInfoMessage extends Message {

    private final long blockCount;

    public BlockChainInfoMessage(long blockCount) {
        super(MessageType.BLOCK_CHAIN_INFO);

        this.blockCount = blockCount;
    }

}
