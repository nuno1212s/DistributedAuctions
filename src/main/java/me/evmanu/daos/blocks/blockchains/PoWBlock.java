package me.evmanu.daos.blocks.blockchains;

import lombok.Getter;
import me.evmanu.daos.blocks.Block;
import me.evmanu.daos.blocks.BlockChain;
import me.evmanu.daos.blocks.BlockChainStandards;
import me.evmanu.daos.blocks.BlockHeader;
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

    private final BigInteger workProof;

    public PoWBlock(BlockHeader header, LinkedHashMap<ByteWrapper, Transaction> transactions,
                    BigInteger workProof) {
        super(header, transactions);

        this.workProof = workProof;
    }

    @Override
    public boolean isValid(BlockChain chain) {

        final var blockHash = getHeader().getBlockHash();

        return ByteHelper.hasFirstBitsSetToZero(blockHash, BlockChainStandards.ZEROS_REQUIRED);
    }

    @Override
    protected void sub_addToHash(MessageDigest hash) {

        hash.update(workProof.toByteArray());

    }
}
