package me.evmanu.daos.blocks.blockbuilders;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class PoWBlockBuilder extends BlockBuilder {

    protected ThreadLocal<Long> workProof;

    public PoWBlockBuilder(long block_number, short version, byte[] previousBlockHash) {
        super(block_number, version, previousBlockHash);
    }

    /**
     * Every thread has it's own counter that should be allocated appropriately by the thread pool (Should have no threads
     * with overlapping counters)
     */
    protected long incrementWorkProof() {
        long bigInteger = workProof.get() + 1;

        workProof.set(bigInteger);

        return bigInteger;
    }

    @Override
    protected void sub_addToHash(MessageDigest hash) {

        final var buffer = ByteBuffer.allocate(Long.BYTES);

        buffer.putLong(workProof.get());

        hash.update(buffer);

    }
}
