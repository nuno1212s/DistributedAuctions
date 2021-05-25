package me.evmanu.daos.blocks.blockchains;

import me.evmanu.daos.blocks.Block;
import me.evmanu.daos.blocks.BlockChain;
import me.evmanu.daos.transactions.Transaction;
import me.evmanu.daos.transactions.TransactionType;
import me.evmanu.util.ByteWrapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PoWBlockChain extends BlockChain {

    public PoWBlockChain(long blockCount, short version, List<Block> blocks) {
        super(blockCount, version, blocks);
    }

    @Override
    protected boolean verifyTransaction(Transaction transaction, long currentlyExaminingBlock,
                                        LinkedHashMap<ByteWrapper, Transaction> blockTransactions) {

        if (transaction.getType() == TransactionType.STAKE || transaction.getType() == TransactionType.STAKE_WITHDRAWAL) {

            System.out.println("Detected stake or stake withdrawal transaction in proof of work block chain!");

            return false;
        }

        return super.verifyTransaction(transaction, currentlyExaminingBlock, blockTransactions);
    }

    public boolean verifyBlock(Block block) {

        if (!(block instanceof PoWBlock)) {
            System.out.println("PoW block chain requires PoW blocks!");

            return false;
        }

        if (!block.verifyBlockID()) {
            System.out.println("The block hash does not match the content of the block.");
            return false;
        }

        if (!block.isValid(this)) {
            System.out.println("The block does not have the correct proof of work.");

            return false;
        }

        var blockHeader = block.getHeader();

        var blockNumber = blockHeader.getBlockNumber();

        Block prevBlock = null;

        if (blockNumber > 0) {

            if (blockNumber <= getBlockCount() && !canForkAt(blockNumber)) {

                System.out.println("This block chain is already further ahead of the block received.");

                return false;
            } else {
                //TODO: When this happens, we have to fork the chain
            }

            prevBlock = getBlockByNumber(blockNumber - 1);
        }

        if (!block.verifyPreviousBlockHash(prevBlock)) {
            System.out.println("Previous block hash is not correct");
            return false;
        }

        if (!block.verifyMerkleRoot()) {
            return false;
        }

        LinkedHashMap<ByteWrapper, Transaction> verifiedTransactions = new LinkedHashMap<>();

        //We want to gradually verify transactions, including the transactions that have already been
        //Verified, so that there is no double spending possibility inside the block
        for (Map.Entry<ByteWrapper, Transaction> transaction : block.getTransactions().entrySet()) {
            if (!verifyTransaction(transaction.getValue(), block.getHeader().getBlockNumber(), verifiedTransactions)) {
                return false;
            }

            verifiedTransactions.put(transaction.getKey(), transaction.getValue());
        }

        return true;
    }

    @Override
    public BlockChain fork(long blockForkNumber)  {

        List<Block> previousBlocks = new ArrayList<>((int) (blockForkNumber + 1));

        for (long block = 0; block < blockForkNumber; block++) {
            //We want to maintain the pointer to the previous blocks,
            //Not copy them, as they are exactly the same, only the following blocks might
            //Be different
            previousBlocks.add(getBlockByNumber(block));
        }

        return new PoWBlockChain(previousBlocks.size(), getVersion(), previousBlocks);
    }
}
