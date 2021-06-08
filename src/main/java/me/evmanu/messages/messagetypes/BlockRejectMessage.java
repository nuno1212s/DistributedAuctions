package me.evmanu.messages.messagetypes;

import lombok.Getter;
import me.evmanu.messages.Message;
import me.evmanu.messages.MessageContent;
import me.evmanu.messages.MessageType;

@Getter
public class BlockRejectMessage extends MessageContent {

    private final byte[] publisher;

    private final long blockNum;

    private final byte[] blockHash;

    public BlockRejectMessage(byte[] publisher, long blockNum, byte[] blockHash) {
        this.publisher = publisher;

        this.blockNum = blockNum;
        this.blockHash = blockHash;
    }

    @Override
    public MessageType getType() {
        return (MessageType.BLOCK_REJECT);
    }
}
