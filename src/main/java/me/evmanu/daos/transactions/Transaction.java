package me.evmanu.daos.transactions;

import lombok.Getter;
import me.evmanu.daos.Hashable;
import me.evmanu.daos.Signable;
import me.evmanu.util.Hex;

import java.nio.ByteBuffer;
import java.security.*;
import java.util.Arrays;
import java.util.Objects;

/**
 * Transaction element that is thread-safe as it is immutable (Same as {@link me.evmanu.daos.blocks.Block}
 *
 * We don't need to have a "unique" identifier for each transaction as even if the outputs are exactly the same as another
 * transaction (Which is completely legal) we can never have the same input (No double spending). There is no way to use the same
 * Input twice, if we only want to use part of input we must
 */
@Getter
public class Transaction implements Hashable, Signable {

    private final byte[] txID;

    private final long blockNumber;

    private final TransactionType type;

    private final short version;

    private final ScriptSignature[] inputs;

    private final ScriptPubKey[] outputs;

    public Transaction(long blockNumber, short version,
            TransactionType type, ScriptSignature[] inputs, ScriptPubKey[] outputs) {
        this.version = version;
        this.type = type;
        this.blockNumber = blockNumber;
        this.inputs = inputs;
        this.outputs = outputs;

        this.txID = calculateTXIDFor(this);
    }

    public boolean verifyTransactionID() {
        return Arrays.equals(txID, calculateTXIDFor(this));
    }

    @Override
    public void addToSignature(Signature signature) {
        var buffer = ByteBuffer.allocate(Short.BYTES + Long.BYTES + Integer.BYTES);

        buffer.putShort(version);
        buffer.putLong(blockNumber);
        buffer.putInt(this.type.ordinal());

        try {
            signature.update(buffer.array());
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        for (ScriptSignature input : inputs) {
            input.addToSignature(signature);
        }

        for (ScriptPubKey output : outputs) {
            output.addToSignature(signature);
        }
    }

    @Override
    public void addToHash(MessageDigest hash) {

        var buffer = ByteBuffer.allocate(Short.BYTES + Long.BYTES);

        buffer.putShort(version);
        buffer.putLong(blockNumber);

        hash.update(buffer.array());

        for (ScriptSignature input : inputs) {
            input.addToHash(hash);
        }

        for (ScriptPubKey output : outputs) {
            output.addToHash(hash);
        }
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "txID=" + Hex.toHexString(this.txID)+
                ", blockNumber=" + blockNumber +
                ", version=" + version +
                ", inputs=" + Arrays.toString(inputs) +
                ", outputs=" + Arrays.toString(outputs) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return getBlockNumber() == that.getBlockNumber() && getVersion() == that.getVersion()
                && Arrays.equals(getTxID(), that.getTxID());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getBlockNumber(), getVersion());
        result = 31 * result + Arrays.hashCode(getTxID());
        return result;
    }

    public static byte[] calculateTXIDFor(Transaction transaction) {
        return Hashable.calculateHashOf(transaction);
    }

}
