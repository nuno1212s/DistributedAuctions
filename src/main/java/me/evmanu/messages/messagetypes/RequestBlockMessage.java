package me.evmanu.messages.messagetypes;

import lombok.Getter;
import me.evmanu.messages.Message;
import me.evmanu.messages.MessageContent;
import me.evmanu.messages.MessageType;

@Getter
public class RequestBlockMessage extends MessageContent {

    private final long blockID;

    private final byte[] previousBlockHash;

    public RequestBlockMessage(long blockID, byte[] previousBlockHash) {
        this.blockID = blockID;
        this.previousBlockHash = previousBlockHash;
    }


    @Override
    public MessageType getType() {
        return MessageType.REQUEST_BLOCK;
    }
}
