package me.evmanu.daos.blocks.blockbuilders;

import lombok.Getter;
import me.evmanu.daos.Hashable;
import me.evmanu.daos.blocks.Block;
import me.evmanu.daos.transactions.Transaction;
import me.evmanu.util.Hex;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public abstract class BlockBuilder implements Hashable, Cloneable {

    @Getter
    private final long block_number;

    private final short version;

    @Getter
    private final byte[] previousBlockHash;

    private AtomicReference<byte[]> merkleRoot;

    /**
     * This is thread-safe as the map contained is never altered, only copied and then the reference is updated
     */
    private AtomicReference<LinkedHashMap<byte[], Transaction>> transactions;

    private AtomicLong timeGenerated;

    public BlockBuilder(long block_number, short version, byte[] previousBlockHash) {
        this.block_number = block_number;
        this.version = version;
        this.previousBlockHash = previousBlockHash;
        this.transactions = new AtomicReference<>(new LinkedHashMap<>());
        this.merkleRoot = new AtomicReference<>(calculateMerkleRoot(this.transactions.get()));
        this.timeGenerated = new AtomicLong(System.currentTimeMillis());
    }

    /**
     * When this method is called, we assume that this transaction has already been verified.
     * @param transaction
     */
    public void addTransaction(Transaction transaction) {

        LinkedHashMap<byte[], Transaction> transactions, newTransactions;

        do {

            transactions = this.transactions.get();

            newTransactions = new LinkedHashMap<>(transactions);

            newTransactions.put(transaction.getTxID(), transaction);

        } while (!this.transactions.compareAndSet(transactions, newTransactions));

        this.timeGenerated.set(System.currentTimeMillis());

    }

    public boolean hasTransaction(byte[] txID) {
        return getTransactionsCurrentlyInBlock().containsKey(txID);
    }

    public LinkedHashMap<byte[], Transaction> getTransactionsCurrentlyInBlock() {
        return this.transactions.get();
    }

    public Transaction getTransactionByID(byte[] txID) {
        return getTransactionsCurrentlyInBlock().get(txID);
    }

    private byte[] calculateMerkleRoot(LinkedHashMap<byte[], Transaction> transactions) {
        //TODO:
        return new byte[0];
    }

    /**
     * Instead of allowing overloads to the block hash, making this method always be executed after we finish adding the
     * generic block information, we maintain a constant order even with updates
     *
     * @param hash
     */
    protected abstract void sub_addToHash(MessageDigest hash);

    @Override
    public final void addToHash(MessageDigest hash) {

        var buffer = ByteBuffer.allocate(Long.BYTES);

        hash.update(buffer.putLong(block_number));

        buffer.clear();

        //TODO: Does this even work? The memory is 8 bytes but I'm only filling up 2. Does he self adjust?
        //Is this world real?
        buffer.putShort(this.version);
        hash.update(buffer);

        buffer.clear();
        buffer.putLong(timeGenerated.get());

        hash.update(buffer);

        hash.update(this.previousBlockHash);

        hash.update(this.merkleRoot.get());

        transactions.get().forEach((txID, transaction) ->  {

            transaction.addToHash(hash);

        });

        sub_addToHash(hash);
    }

    public abstract Block build();

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
