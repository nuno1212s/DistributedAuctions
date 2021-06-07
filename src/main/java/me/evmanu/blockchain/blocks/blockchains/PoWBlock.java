package me.evmanu.blockchain.blocks.blockchains;

import lombok.Getter;
import me.evmanu.blockchain.blocks.*;
import me.evmanu.blockchain.transactions.Transaction;
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

    @Override
    public BlockType getBlockType() {
        return BlockType.PROOF_OF_WORK;
    }
}
