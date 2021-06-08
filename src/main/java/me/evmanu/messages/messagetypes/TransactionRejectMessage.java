package me.evmanu.messages.messagetypes;

import lombok.Getter;
import me.evmanu.messages.MessageContent;
import me.evmanu.messages.MessageType;

@Getter
public class TransactionRejectMessage extends MessageContent {

    private final byte[] txID;

    public TransactionRejectMessage(byte[] txID) {
        this.txID = txID;
    }

    @Override
    public MessageType getType() {
        return MessageType.TRANSACTION_REJECT;
    }
}
