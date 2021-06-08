package me.evmanu.messages.messagetypes;

import lombok.Getter;
import me.evmanu.messages.MessageContent;
import me.evmanu.messages.MessageType;

@Getter
public class BidMessage extends MessageContent {

    private final byte[] bidID;

    public BidMessage(byte[] bidID) {
        this.bidID = bidID;
    }

    @Override
    public MessageType getType() {
        return MessageType.BID_ANNOUNCE;
    }
}
