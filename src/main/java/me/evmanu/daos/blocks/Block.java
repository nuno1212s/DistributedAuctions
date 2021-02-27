package me.evmanu.daos.blocks;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.evmanu.Standards;
import me.evmanu.daos.Hashable;
import me.evmanu.daos.transactions.MerkleVerifiableTransaction;
import me.evmanu.daos.transactions.Transaction;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashMap;

/*
 * This block is a thread-safe stored block that has already been added to the block-chain. It cannot be altered only
 * accessed so it doesn't need locking or synchronization
 */
@AllArgsConstructor
@Getter
public abstract class Block implements Hashable {

    protected final BlockHeader header;

    /**
     * All the transactions in the block, indexed by their transaction ID.
     *
     * This uses a LinkedHashMap so we maintain ordering for the blocks
     */
    protected final LinkedHashMap<byte[], Transaction> transactions;

    public final boolean verifyBlockID() {

        final var digest = Standards.getDigestInstance();

        assert digest != null;

        this.addToHash(digest);

        if (!Arrays.equals(digest.digest(), header.getBlockHash())) {
            System.out.println("Failed to verify block hash for block " + this);
            return false;
        }

        return true;
    }

    public final Transaction getTransactionByID(byte[] txID) {
        return this.transactions.get(txID);
    }

    public final boolean verifyTransactionIsInBlock(byte[] txID) {
        return this.transactions.containsKey(txID);
    }

    public final boolean verifyTransactionIsInBlock(Transaction transaction) {
        if (transaction instanceof MerkleVerifiableTransaction) {
            if (!this.header.verifyMerkleTree((MerkleVerifiableTransaction) transaction)) {
                return false;
            }
        }

        return verifyTransactionIsInBlock(transaction.getTxID());
    }

    @Override
    public final void addToHash(MessageDigest hash) {

        var buffer = ByteBuffer.allocate(Long.BYTES);

        hash.update(buffer.putLong(header.getBlockNumber()));

        buffer.clear();

        //TODO: Does this even work? The memory is 8 bytes but I'm only filling up 2. Does he self adjust?
        //Is this world real?
        buffer.putShort(header.getVersion());
        hash.update(buffer);

        buffer.clear();
        buffer.putLong(header.getTimeGenerated());

        hash.update(buffer);

        hash.update(header.getPreviousBlockHash());

        hash.update(header.getMerkleRoot());

        transactions.forEach((txID, transaction) -> transaction.addToHash(hash));

        sub_addToHash(hash);
    }

    /**
     * Instead of allowing overloads to the block hash, making this method always be executed after we finish adding the
     * generic block information, we maintain a constant order even with updates
     *
     * @param hash
     */
    protected abstract void sub_addToHash(MessageDigest hash);



}