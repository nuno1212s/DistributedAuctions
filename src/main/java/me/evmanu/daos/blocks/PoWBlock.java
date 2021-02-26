package me.evmanu.daos.blocks;

import lombok.Getter;
import me.evmanu.daos.transactions.Transaction;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.LinkedHashMap;

/**
 * This represents a built block, part of the blockchain with an assigned work Proof
 */
@Getter
public class PoWBlock extends Block {

    private final BigInteger workProof;

    public PoWBlock(BlockHeader header, LinkedHashMap<byte[], Transaction> transactions,
                    BigInteger workProof) {
        super(header, transactions);

        this.workProof = workProof;
    }

    @Override
    protected void sub_addToHash(MessageDigest hash) {

        hash.update(workProof.toByteArray());

    }
}
