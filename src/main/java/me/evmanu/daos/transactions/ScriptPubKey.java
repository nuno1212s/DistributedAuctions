package me.evmanu.daos.transactions;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.evmanu.daos.Hashable;
import me.evmanu.util.Hex;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;

@Getter
@AllArgsConstructor(access = AccessLevel.PUBLIC)
/*
 * This is part of the P2PKH (Pay To PubKey Hash) protocol
 */
public class ScriptPubKey implements Hashable {

    private final byte[] hashedPubKey;

    private final float amount;

    @Override
    public void addToHash(MessageDigest digest) {

        digest.update(this.hashedPubKey);

        //Floats have 4 bytes
        var buffer = ByteBuffer.allocate(Float.BYTES);

        buffer.putFloat(amount);

        digest.update(buffer.array());
    }

    @Override
    public String toString() {
        return "Output{" +
                "hashedPubKey=" + Hex.toHexString(hashedPubKey) +
                ", amount=" + amount +
                '}';
    }
}
