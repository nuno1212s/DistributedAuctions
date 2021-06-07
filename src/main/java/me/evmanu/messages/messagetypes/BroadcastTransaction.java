package me.evmanu.messages.messagetypes;

import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.messages.Message;
import me.evmanu.messages.MessageType;

public class BroadcastTransaction extends Message {

    private Transaction transaction;

    public BroadcastTransaction() {
        super(MessageType.BROADCAST_TRANSACTION);
    }
}
