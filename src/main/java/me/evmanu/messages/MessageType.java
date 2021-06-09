package me.evmanu.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.evmanu.messages.messagetypes.*;

@AllArgsConstructor
@Getter
public enum MessageType {

    BROADCAST_TRANSACTION(TransactionMessage.class),
    BROADCAST_BLOCK(BlockMessage.class),
    REQUEST_BLOCK_CHAIN(BlockChainRequestMessage.class),
    BLOCK_CHAIN_INFO(BlockChainInfoMessage.class),
    REQUEST_BLOCK(RequestBlockMessage.class),
    BLOCK_REJECT(BlockRejectMessage.class),
    TRANSACTION_REJECT(TransactionRejectMessage.class),
    AUCTION_ANNOUNCE(AuctionMessage.class),
    AUCTION_REQUEST(ActiveAuctionRequestMessage.class),
    BID_ANNOUNCE(BidMessage.class),
    REQUEST_PAYMENT(RequestPaymentMessage.class);

    private final Class<? extends MessageContent> typeClass;

}
