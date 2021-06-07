package me.evmanu.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.evmanu.messages.messagetypes.BroadcastTransaction;

@AllArgsConstructor
@Getter
public enum MessageType {

    BROADCAST_TRANSACTION(BroadcastTransaction.class);

    private Class<? extends Message> typeClass;

}
