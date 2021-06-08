package me.evmanu.blockchain.blocks.blockbuilders;

import lombok.Getter;
import me.evmanu.blockchain.Hashable;
import me.evmanu.blockchain.Signable;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.util.ByteWrapper;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.SignatureException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public abstract class BlockBuilder implements Hashable, Signable, Cloneable {

    @Getter
    private final long blockNumber;

    @Getter
    private final short version;

    @Getter
    private final byte[] previousBlockHash;

    private AtomicReference<byte[]> merkleRoot;

    /**
     * This is thread-safe as the map contained is never altered, only copied and then the reference is updated
     */
    private AtomicReference<LinkedHashMap<ByteWrapper, Transaction>> transactions;

    protected AtomicLong timeGenerated;

    public BlockBuilder(long blockNumber, short version, byte[] previousBlockHash) {
        this.blockNumber = blockNumber;
        this.version = version;
        this.previousBlockHash = previousBlockHash;
        this.transactions = new AtomicReference<>(new LinkedHashMap<>());
        this.merkleRoot = new AtomicReference<>(calculateMerkleRoot(this.transactions.get()));
        this.timeGenerated = new AtomicLong(System.currentTimeMillis());
    }


    /**
     * In this method we assume the transactions have been verified
     *
     * @param transactions
     */
    public void setTransactions(List<Transaction> transactions) {

        LinkedHashMap<ByteWrapper, Transaction> transactionList = new LinkedHashMap<>();

        for (Transaction transaction : transactions) {
            transactionList.put(new ByteWrapper(transaction.getTxID()), transaction);
        }

        this.transactions.set(transactionList);
    }

    /**
     * When this method is called, we assume that this transaction has already been verified.
     *
     * @param transaction
     */
    public void addTransaction(Transaction transaction) {

        LinkedHashMap<ByteWrapper, Transaction> transactions, newTransactions;

        do {

            transactions = this.transactions.get();

            newTransactions = new LinkedHashMap<>(transactions);

            newTransactions.put(new ByteWrapper(transaction.getTxID()), transaction);

        } while (!this.transactions.compareAndSet(transactions, newTransactions));

        this.timeGenerated.set(System.currentTimeMillis());

    }

    public boolean hasTransaction(byte[] txID) {
        return getTransactionsCurrentlyInBlock().containsKey(new ByteWrapper(txID));
    }

    public LinkedHashMap<ByteWrapper, Transaction> getTransactionsCurrentlyInBlock() {
        return this.transactions.get();
    }

    public Transaction getTransactionByID(byte[] txID) {
        return getTransactionsCurrentlyInBlock().get(new ByteWrapper(txID));
    }

    protected byte[] getMerkleRoot() {
        return this.merkleRoot.get();
    }

    private byte[] calculateMerkleRoot(LinkedHashMap<ByteWrapper, Transaction> transactions) {
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

    protected abstract void sub_addToSignature(Signature signature) throws SignatureException;

    @Override
    public final void addToHash(MessageDigest hash) {

        var buffer = ByteBuffer.allocate(Long.BYTES);

        hash.update(buffer.putLong(blockNumber).array());

        buffer.clear();

        //TODO: Does this even work? The memory is 8 bytes but I'm only filling up 2. Does he self adjust?
        //Is this world real?
        buffer.putShort(this.version);
        hash.update(buffer.array());

        buffer.clear();
        buffer.putLong(timeGenerated.get());

        hash.update(buffer.array());

        hash.update(this.previousBlockHash);

        hash.update(this.merkleRoot.get());

        transactions.get().forEach((txID, transaction) -> {

            transaction.addToHash(hash);

        });

        sub_addToHash(hash);
    }

    @Override
    public void addToSignature(Signature signature) throws SignatureException {

        var buffer = ByteBuffer.allocate(Long.BYTES);

        signature.update(buffer.putLong(blockNumber).array());

        buffer.clear();

        //TODO: Does this even work? The memory is 8 bytes but I'm only filling up 2. Does he self adjust?
        //Is this world real?
        buffer.putShort(this.version);
        signature.update(buffer.array());

        buffer.clear();
        buffer.putLong(timeGenerated.get());

        signature.update(buffer.array());

        signature.update(this.previousBlockHash);

        signature.update(this.merkleRoot.get());

        transactions.get().forEach((txID, transaction) -> {

            transaction.addToSignature(signature);

        });

        sub_addToSignature(signature);
    }

    public abstract Block build();

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
