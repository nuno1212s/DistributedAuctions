package me.evmanu.daos.blocks;

import lombok.Getter;
import me.evmanu.daos.transactions.Transaction;
import me.evmanu.util.ByteHelper;
import me.evmanu.util.ByteWrapper;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.LinkedHashMap;

/**
 * This represents a built block, part of the blockchain with an assigned work Proof
 */
@Getter
public class PoWBlock extends Block {

    //TODO: Mara, usa esta variável para ver os zeros que são precisos
    public static final int ZEROS_REQUIRED = 1;

    private final BigInteger workProof;

    public PoWBlock(BlockHeader header, LinkedHashMap<ByteWrapper, Transaction> transactions,
                    BigInteger workProof) {
        super(header, transactions);

        this.workProof = workProof;
    }

    @Override
    public boolean isValid() {

        final var blockHash = getHeader().getBlockHash();

        return ByteHelper.hasFirstBitsSetToZero(blockHash, ZEROS_REQUIRED);
    }

    @Override
    protected void sub_addToHash(MessageDigest hash) {

        hash.update(workProof.toByteArray());

    }
}
