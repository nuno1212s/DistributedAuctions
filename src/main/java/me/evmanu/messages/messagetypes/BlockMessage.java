package me.evmanu.messages.messagetypes;

import lombok.Getter;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.messages.Message;
import me.evmanu.messages.MessageContent;
import me.evmanu.messages.MessageType;

@Getter
public class BlockMessage extends MessageContent {

    private final Block block;

    public BlockMessage(Block block) {
        this.block = block;
    }

    @Override
    public MessageType getType() {
        return MessageType.BROADCAST_BLOCK;
    }
}
