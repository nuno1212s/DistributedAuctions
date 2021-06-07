package me.evmanu.messages.messagetypes;

import me.evmanu.messages.Message;
import me.evmanu.messages.MessageType;

public class BlockChainInfoRequestMessage extends Message {

    public BlockChainInfoRequestMessage() {
        super(MessageType.REQUEST_BLOCK_CHAIN);
    }
}
