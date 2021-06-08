package me.evmanu.messages.messagetypes;

import lombok.Getter;
import me.evmanu.messages.Message;
import me.evmanu.messages.MessageType;

@Getter
public class RequestBlockMessage extends Message {

    private final long blockID;

    private final byte[] previousBlockHash;

    public RequestBlockMessage(long blockID, byte[] previousBlockHash) {
        super(MessageType.REQUEST_BLOCK);

        this.blockID = blockID;
        this.previousBlockHash = previousBlockHash;
    }

}
