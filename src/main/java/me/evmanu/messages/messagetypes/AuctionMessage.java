package me.evmanu.messages.messagetypes;

import lombok.Getter;
import me.evmanu.auctions.Auction;
import me.evmanu.messages.MessageContent;
import me.evmanu.messages.MessageType;

@Getter
public class AuctionMessage extends MessageContent {

    private final byte[] auctionID;

    public AuctionMessage(byte[] auction) {
        this.auctionID = auction;
    }

    @Override
    public MessageType getType() {
        return MessageType.AUCTION_ANNOUNCE;
    }
}
