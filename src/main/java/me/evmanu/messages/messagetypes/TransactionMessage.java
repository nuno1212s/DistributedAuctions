package me.evmanu.messages.messagetypes;

import lombok.Getter;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.messages.Message;
import me.evmanu.messages.MessageContent;
import me.evmanu.messages.MessageType;

@Getter
public class TransactionMessage extends MessageContent {

    private final Transaction transaction;

    public TransactionMessage(Transaction transaction) {
        this.transaction = transaction;
    }


    @Override
    public MessageType getType() {
        return MessageType.BROADCAST_TRANSACTION;
    }
}
