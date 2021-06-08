package me.evmanu.messages.messagetypes;

import lombok.Getter;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.messages.Message;
import me.evmanu.messages.MessageType;

@Getter
public class BlockMessage extends Message {

    private final Block block;

    public BlockMessage(Block block) {
        super(MessageType.BROADCAST_BLOCK);

        this.block = block;
    }
}
