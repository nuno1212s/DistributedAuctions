package me.evmanu.messages.messagetypes;

import lombok.Getter;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.messages.Message;
import me.evmanu.messages.MessageType;

@Getter
public class TransactionMessage extends Message {

    private final Transaction transaction;

    public TransactionMessage(Transaction transaction) {
        super(MessageType.BROADCAST_TRANSACTION);

        this.transaction = transaction;
    }

}
