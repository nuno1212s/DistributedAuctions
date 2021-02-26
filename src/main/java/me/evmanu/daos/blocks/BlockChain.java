package me.evmanu.daos.blocks;

import lombok.Getter;
import me.evmanu.daos.blocks.blockbuilders.BlockBuilder;
import me.evmanu.daos.transactions.ScriptSignature;
import me.evmanu.daos.transactions.Transaction;

import java.util.List;

@Getter
public class BlockChain {

    protected long blockCount;

    /**
     * Blocks indexed by their block number
     */
    protected List<Block> blocks;

    /**
     * All clients, even if they're not miners store the current block, made up of transactions that they have picked up
     * so they can run quick verifications on transactions that are meant for them
     */
    protected BlockBuilder currentBlock;

    public BlockChain(long blockCount, List<Block> blocks) {
        this.blockCount = blockCount;
        this.blocks = blocks;
    }

    public Block getBlockByNumber(long blockNumber) {

        //For this assignment, I think we can allow for a limit of 2^32 blocks.
        //But, TODO: Change this to allow for larger block sizes
        return blocks.get(((Long) blockNumber).intValue());
    }

    //TODO: Add the verification of new blocks

    public boolean verifyTransaction(Transaction transaction) {

        if (!transaction.verifyTransactionID()) {
            return false;
        }

        final var inputs = transaction.getInputs();

        for (ScriptSignature input : inputs) {

            final var originatingBlock = getBlockByNumber(input.getOriginatingBlock());

            if (!originatingBlock.verifyTransactionIsInBlock(input.getOriginatingTXID())) {
                return false;
            }

            final var parentT = originatingBlock.getTransactionByID(input.getOriginatingTXID());

            if (!input.verifyOutputExists(parentT)) {
                return false;
            }

        }

        return true;
    }

}
