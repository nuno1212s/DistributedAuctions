package me.evmanu.daos.blocks;

import lombok.Getter;
import me.evmanu.daos.transactions.Transaction;
import me.evmanu.util.ByteHelper;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.LinkedHashMap;

/**
 * This represents a built block, part of the blockchain with an assigned work Proof
 */
@Getter
public class PoWBlock extends Block {

    //TODO: Mara, usa esta variável para ver os zeros que são precisos
    public static final int ZEROS_REQUIRED = 0;

    private final BigInteger workProof;

    public PoWBlock(BlockHeader header, LinkedHashMap<byte[], Transaction> transactions,
                    BigInteger workProof) {
        super(header, transactions);

        this.workProof = workProof;
    }

    @Override
    public boolean isValid() {

        final var blockHash = getHeader().getBlockHash();

        for (int block = 0; block < (int) Math.ceil(ZEROS_REQUIRED / (float) Byte.SIZE); block++) {
            byte byteBlock = blockHash[block];

            final var byteWithFirstOnes = ByteHelper.getByteWithFirstOnes(ZEROS_REQUIRED);

            //TO check if the first x bits are 0, we do an and with a byte where the first x bits are 1,
            //And the rest are 0, so when we AND them, we must get 0
            //Example: 0001 1111 & 1110 0000 = 0
            if ((byteBlock & byteWithFirstOnes) != 0) {
                return false;
            }

        }

        return true;
    }

    @Override
    protected void sub_addToHash(MessageDigest hash) {

        hash.update(workProof.toByteArray());

    }
}
