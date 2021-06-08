package me.evmanu.blockchain.blocks.blockbuilders;

import lombok.Setter;
import me.evmanu.blockchain.Hashable;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.blocks.BlockHeader;
import me.evmanu.blockchain.blocks.blockchains.PoWBlock;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.util.Hex;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.SignatureException;
import java.util.List;

public class PoWBlockBuilder extends BlockBuilder {

    @Setter
    protected long workProof;

    public PoWBlockBuilder(long block_number, short version, byte[] previousBlockHash) {
        super(block_number, version, previousBlockHash);
    }

    /**
     * Every thread has it's own counter that should be allocated appropriately by the thread pool (Should have no threads
     * with overlapping counters)
     */
    protected long incrementWorkProof() {
        return workProof++;
    }

    @Override
    public Block build() {

        byte[] blockHash = Hashable.calculateHashOf(this);

        System.out.println(workProof + " " + Hex.toHexString(blockHash));

        var header = new BlockHeader(
                getBlockNumber(),
                getVersion(),
                timeGenerated.get(),
                getPreviousBlockHash(),
                blockHash,
                new byte[0]
        );

        return new PoWBlock(header,
                getTransactionsCurrentlyInBlock(),
                this.workProof);
    }

    @Override
    protected void sub_addToSignature(Signature signature) throws SignatureException {

        final var buffer = ByteBuffer.allocate(Long.BYTES);

        buffer.putLong(workProof);

        signature.update(buffer.array());

    }

    @Override
    protected void sub_addToHash(MessageDigest hash) {

        final var buffer = ByteBuffer.allocate(Long.BYTES);

        buffer.putLong(workProof);

        hash.update(buffer.array());

    }

    public static PoWBlockBuilder fromTransactionList(long blockNum, short version, byte[] prevHash,
                                                      List<Transaction> transactions) {

        PoWBlockBuilder poWBlockBuilder = new PoWBlockBuilder(blockNum, version, prevHash);

        poWBlockBuilder.setTransactions(transactions);

        return poWBlockBuilder;
    }
}
