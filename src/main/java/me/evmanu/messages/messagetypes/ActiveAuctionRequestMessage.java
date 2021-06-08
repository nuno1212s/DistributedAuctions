package me.evmanu.messages.messagetypes;

import me.evmanu.messages.MessageContent;
import me.evmanu.messages.MessageType;

public class ActiveAuctionRequestMessage extends MessageContent {

    @Override
    public MessageType getType() {
        return MessageType.AUCTION_REQUEST;
    }
}
