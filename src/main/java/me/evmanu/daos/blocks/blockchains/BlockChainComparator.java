package me.evmanu.daos.blocks.blockchains;

import me.evmanu.daos.blocks.Block;
import me.evmanu.daos.blocks.BlockChain;
import me.evmanu.daos.transactions.Transaction;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * A comparator for comparing two block chains so we can reach a network wide
 * consensus on the block chain that everyone should be using
 * <p>
 * This comparator will never return a 0 (Equal block chains), there is
 * always a block chain that is better than the other
 */
public class BlockChainComparator implements Comparator<BlockChain> {

    @Override
    public int compare(BlockChain o1, BlockChain o2) {

        assert o1.getClass().equals(o2.getClass());

        int blockSizeComparison = Long.compare(o1.getBlockCount(), o2.getBlockCount());

        if (blockSizeComparison == 0) {

            long lastCommonBlock = checkBlockChainDiff(o1, o2);

            //In case of a tie, we have to use a tie breaker
            if (o1 instanceof PoSBlockChain) {
                PoSBlockChain c1 = (PoSBlockChain) o1, c2 = (PoSBlockChain) o2;

                PoSBlock b1 = (PoSBlock) c1.getBlockByNumber(lastCommonBlock + 1),
                        b2 = (PoSBlock) c2.getBlockByNumber(lastCommonBlock + 1);

                byte[] signature = b1.getSignature(), signature2 = b2.getSignature();

                /*
                Compare the signature of the block succeeding the last common block,
                (Signatures are compare by the order they show up on the allowed signers list,
                So if you show up first on the list you have the priority)
                */
                List<Transaction> allowedSigners1 = c1.getAllowedSignersForBlock(b1.getHeader().getBlockNumber()),
                        allowedSigners2 = c2.getAllowedSignersForBlock(b2.getHeader().getBlockNumber());

                assert allowedSigners1.equals(allowedSigners2);

                for (Transaction transaction : allowedSigners1) {
                    if (Arrays.equals(transaction.getTxID(), signature)) {
                        //If the transaction c1 is first in the signature list, then it has higher priority
                        //So it should be first on the list
                        return -1;
                    } else if (Arrays.equals(transaction.getTxID(), signature2)) {
                        return 1;
                    }
                }
            } else {

                /*
                When in proof of work, we choose the block chain that has the smallest BigInteger work proof
                So effectively, we choose the work proof with the largest amount of zeros preceding it
                */

                PoWBlock b1 = (PoWBlock) o1.getBlockByNumber(lastCommonBlock + 1),
                        b2 = (PoWBlock) o2.getBlockByNumber(lastCommonBlock + 1);

                BigInteger wp1 = b1.getWorkProof(), wp2 = b2.getWorkProof();

                int i = wp1.compareTo(wp2);

                if (i == 0) {
                    //They have exactly the same work proof????
                    int compare = Long.compare(b1.getHeader().getTimeGenerated(), b2.getHeader().getTimeGenerated());

                    //Return the block that was generated first
                    if (compare == 0) {
                        //We know that the hash of the blocks has to be different otherwise they would be the same
                        //Block, so in the last resort we compare the hashes of the blocks

                        return Arrays.compare(b1.getHeader().getBlockHash(), b2.getHeader().getBlockHash());
                    }

                    return compare;
                }
                return i;
            }

        }

        //We want the order of the block chains to be:
        //First position has the largest block chain, so we have to reverse the order
        return -blockSizeComparison;
    }

    /**
     * Get the last common block number between the two blockchains
     *
     * @param b1
     * @param b2
     * @return
     */
    private long checkBlockChainDiff(BlockChain b1, BlockChain b2) {

        long blockStart = Math.min(b1.getBlockCount(), b2.getBlockCount());

        for (; blockStart >= 0; blockStart--) {

            Block block1 = b1.getBlockByNumber(blockStart), block2 = b2.getBlockByNumber(blockStart);

            byte[] block1Hash = block1.getHeader().getBlockHash(),
                    block2Hash = block2.getHeader().getBlockHash();

            if (Arrays.equals(block1Hash, block2Hash)) {
                return blockStart;
            }

        }

        return 0;
    }
}
