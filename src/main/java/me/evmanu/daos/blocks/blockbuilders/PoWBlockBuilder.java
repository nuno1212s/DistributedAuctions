package me.evmanu.daos.blocks.blockbuilders;

import lombok.Setter;
import me.evmanu.daos.Hashable;
import me.evmanu.daos.blocks.Block;
import me.evmanu.daos.blocks.BlockHeader;
import me.evmanu.daos.blocks.PoWBlock;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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

        var header = new BlockHeader(
                getBlockNumber(),
                getVersion(),
                System.currentTimeMillis(),
                getPreviousBlockHash(),
                Hashable.calculateHashOf(this),
                new byte[0]
        );

        return new PoWBlock(header,
                getTransactionsCurrentlyInBlock(),
                BigInteger.valueOf(this.workProof));
    }

    @Override
    protected void sub_addToHash(MessageDigest hash) {

        final var buffer = ByteBuffer.allocate(Long.BYTES);

        buffer.putLong(workProof);

        hash.update(buffer);

    }
}
