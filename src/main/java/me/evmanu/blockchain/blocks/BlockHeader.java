package me.evmanu.blockchain.blocks;

import lombok.Getter;
import me.evmanu.blockchain.transactions.MerkleVerifiableTransaction;

@Getter
/*
 * we separated the header from the block class as we want to attempt to implement SPV clients
 * So to avoid spaghetti code, we just abstracted it
 */
public class BlockHeader {

    protected final long blockNumber;

    protected final short version;

    /**
     * The time that the last transaction was inserted into this block
     */
    protected final long timeGenerated;

    /**
     * Because we are using SHA-3, which is resistant to Length-Extension attacks, we don't have to
     */
    protected final byte[] previousBlockHash, blockHash;

    protected final byte[] merkleRoot;

    public BlockHeader(long blockNumber, short version, long timeGenerated,
                       byte[] previousBlockHash, byte[] blockHash, byte[] merkleRoot) {
        this.blockNumber = blockNumber;
        this.version = version;
        this.timeGenerated = timeGenerated;
        this.previousBlockHash = previousBlockHash;
        this.blockHash = blockHash;
        this.merkleRoot = merkleRoot;
    }

    public boolean verifyMerkleTree(MerkleVerifiableTransaction transaction) {
        //TODO: Implement this
        return true;
    }

}
