package me.evmanu.daos.transactions;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.evmanu.daos.Hashable;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)

/*
 * This is part of the P2PKH (Pay To PubKey Hash) protocol
 */
public class ScriptPubKey implements Hashable {

    private final byte[] hashedPubKey;

    private final float amount;

    @Override
    public void addToHash(MessageDigest digest) {
        //Floats have 4 bytes
        var buffer = ByteBuffer.allocate(hashedPubKey.length + 4);

        buffer.put(hashedPubKey);

        buffer.putFloat(amount);

        digest.update(buffer.array());
    }
}
