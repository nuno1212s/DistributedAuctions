package me.evmanu.daos.blocks;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.evmanu.Standards;
import me.evmanu.daos.Hashable;
import me.evmanu.daos.Signable;
import me.evmanu.daos.transactions.MerkleVerifiableTransaction;
import me.evmanu.daos.transactions.Transaction;
import me.evmanu.util.ByteWrapper;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.LinkedHashMap;

/*
 * This block is a thread-safe stored block that has already been added to the block-chain. It cannot be altered only
 * accessed so it doesn't need locking or synchronization
 */
@AllArgsConstructor
@Getter
public abstract class Block implements Hashable, Signable {

    protected final BlockHeader header;

    /**
     * All the transactions in the block, indexed by their transaction ID.
     * <p>
     * This uses a LinkedHashMap so we maintain ordering for the blocks
     */
    protected final LinkedHashMap<ByteWrapper, Transaction> transactions;

    /**
     * Verify that the previous block hash matches
     * The hash of the previous block in the blockchain
     *
     * @param previousBlock
     * @return
     */
    public final boolean verifyPreviousBlockHash(Block previousBlock) {
        if (previousBlock == null) {
            return this.getHeader().getPreviousBlockHash().length == 0;
        }

        return Arrays.equals(this.getHeader().getPreviousBlockHash(),
                previousBlock.getHeader().getBlockHash());
    }

    public final boolean verifyMerkleRoot() {
        return true;
    }

    /**
     * Verify that the block hash of this block is correct
     *
     * @return
     */
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
        return this.transactions.get(new ByteWrapper(txID));
    }

    public final boolean verifyTransactionIsInBlock(byte[] txID) {
        return this.transactions.containsKey(new ByteWrapper(txID));
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

        hash.update(buffer.putLong(header.getBlockNumber()).array());

        buffer.clear();

        //TODO: Does this even work? The memory is 8 bytes but I'm only filling up 2. Does it self adjust?
        //Is this world real?
        buffer.putShort(header.getVersion());
        hash.update(buffer.array());

        buffer.clear();
        buffer.putLong(header.getTimeGenerated());

        hash.update(buffer.array());

        hash.update(header.getPreviousBlockHash());

        hash.update(header.getMerkleRoot());

        transactions.forEach((txID, transaction) -> transaction.addToHash(hash));

        sub_addToHash(hash);
    }

    @Override
    public void addToSignature(Signature signature) {

        var buffer = ByteBuffer.allocate(Long.BYTES);

        try {
            signature.update(buffer.putLong(header.getBlockNumber()).array());

            buffer.clear();

            //TODO: Does this even work? The memory is 8 bytes but I'm only filling up 2. Does it self adjust?
            //Is this world real?
            buffer.putShort(header.getVersion());
            signature.update(buffer.array());

            buffer.clear();
            buffer.putLong(header.getTimeGenerated());

            signature.update(buffer.array());

            signature.update(header.getPreviousBlockHash());

            signature.update(header.getMerkleRoot());

            transactions.forEach((txID, transaction) -> transaction.addToSignature(signature));
        } catch (SignatureException e) {

            e.printStackTrace();
        }
    }

    /**
     * Check if this block is valid, according to a certain block chain.
     *
     * @param chain
     * @return
     */
    public abstract boolean isValid(BlockChain chain);

    /**
     * Instead of allowing overloads to the block hash, making this method always be executed after we finish adding the
     * generic block information, we maintain a constant order even with updates
     *
     * @param hash
     */
    protected abstract void sub_addToHash(MessageDigest hash);


}
