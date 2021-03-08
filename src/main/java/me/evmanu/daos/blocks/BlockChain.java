package me.evmanu.daos.blocks;

import lombok.Getter;
import me.evmanu.daos.Hashable;
import me.evmanu.daos.blocks.blockbuilders.BlockBuilder;
import me.evmanu.daos.blocks.blockbuilders.PoWBlockBuilder;
import me.evmanu.daos.transactions.ScriptPubKey;
import me.evmanu.daos.transactions.ScriptSignature;
import me.evmanu.daos.transactions.Transaction;
import me.evmanu.util.Hex;

import java.util.*;

@Getter
public class BlockChain {

    protected long blockCount;

    protected short version;

    /**
     * Blocks indexed by their block number
     * <p>
     * Every block that is in this list has already been verified and is certain
     * to be correct according to the block chain
     */
    protected List<Block> blocks;

    /**
     * All clients, even if they're not miners store the current block, made up of transactions that they have picked up
     * so they can run quick verifications on transactions that are meant for them
     */
    protected BlockBuilder currentBlock;

    public BlockChain(long blockCount, short version, List<Block> blocks) {
        this.blockCount = blockCount;
        this.version = version;
        this.blocks = blocks;

        long currentBlockID = 0;

        if (!this.blocks.isEmpty()) {
            currentBlockID = this.blocks.get(this.blocks.size() - 1).getHeader().getBlockNumber() + 1;
        }

        this.currentBlock = new PoWBlockBuilder(currentBlockID, version,
                new byte[0]);
    }

    public Block getLatestValidBlock() {
        return getBlockByNumber(this.blockCount - 1);
    }

    public Block getBlockByNumber(long blockNumber) {

        //For this assignment, I think we can allow for a limit of 2^32 blocks.
        //But, TODO: Change this to allow for larger block sizes
        return blocks.get(((Long) blockNumber).intValue());
    }

    //TODO: Add the verification of new blocks
    public boolean verifyBlock(Block block) {

        if (!block.verifyBlockID()) {
            System.out.println("The block hash does not match the content of the block.");
            return false;
        }

        if (!block.isValid()) {
            System.out.println("The block does not have the correct pow/pos.");

            return false;
        }

        var blockHeader = block.getHeader();

        var blockNumber = blockHeader.getBlockNumber();

        Block prevBlock = null;

        if (blockNumber > 0) {

            if (blockNumber <= getBlockCount()) {
                System.out.println("This block chain is already further ahead of the block received.");

                return false;
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

        LinkedHashMap<byte[], Transaction> verifiedTransactions = new LinkedHashMap<>();

        //We want to gradually verify transactions, including the transactions that have already been
        //Verified, so that there is no double spending possibility inside the block
        for (Map.Entry<byte[], Transaction> transaction : block.getTransactions().entrySet()) {
            if (!verifyTransaction(transaction.getValue(), block.getHeader().getBlockNumber(), verifiedTransactions)) {
                return false;
            }

            verifiedTransactions.put(transaction.getKey(), transaction.getValue());
        }

        return true;
    }

    /**
     * Verify that a list of outputs have not been spent in a list of transactions (Usually a block)
     *
     * @param transactions
     * @param outputsToCheck
     * @return
     */
    private boolean verifyDoubleSpendingInTransactions(LinkedHashMap<byte[], Transaction> transactions,
                                                       Map<byte[], List<Integer>> outputsToCheck) {

        for (Map.Entry<byte[], Transaction> transaction : transactions.entrySet()) {

            var trans = transaction.getValue();

            for (ScriptSignature input : trans.getInputs()) {

                if (outputsToCheck.containsKey(input.getOriginatingTXID())) {
                    var outputsTryingToSpend = outputsToCheck.get(input.getOriginatingTXID());

                    if (outputsTryingToSpend.contains(input.getOutputIndex())) {
                        System.out.println("The input coming from the transaction " +
                                Hex.toHexString(input.getOriginatingTXID()) + " with index " +
                                input.getOutputIndex() + " has already been spent in the transaction " +
                                Hex.toHexString(transaction.getKey()));

                        return false;
                    }

                }
            }
        }

        return true;
    }

    /**
     * Verifies if a transaction is double spending
     * And verifies if any of it's inputs
     *
     * @param newTransaction
     * @return
     */
    private boolean verifyDoubleSpending(Transaction newTransaction, long currentBlockBeingChecked,
                                         LinkedHashMap<byte[], Transaction> transactionsBeingVerified) {

        long oldestOriginatingBlock = 0;

        Map<byte[], List<Integer>> outputsToCheck = new HashMap<>();

        for (ScriptSignature input : newTransaction.getInputs()) {

            var originatingTXID = input.getOriginatingTXID();

            var outputsForTrans = outputsToCheck.getOrDefault(originatingTXID, new LinkedList<>());

            outputsForTrans.add(input.getOutputIndex());

            outputsToCheck.put(originatingTXID, outputsForTrans);

            if (oldestOriginatingBlock < input.getOriginatingBlock()) {
                oldestOriginatingBlock = input.getOriginatingBlock();
            }
        }

        for (; oldestOriginatingBlock < blockCount; oldestOriginatingBlock++) {
            var blockByNumber = getBlockByNumber(oldestOriginatingBlock);

            if (blockByNumber.verifyTransactionIsInBlock(newTransaction.getTxID())) {
                System.out.println("Attempting to verify transaction that is already contained in the blockchain");
                return false;
            }

            if (!verifyDoubleSpendingInTransactions(blockByNumber.getTransactions(), outputsToCheck)) {
                return false;
            }
        }


        if (currentBlockBeingChecked > 0) {

            return verifyDoubleSpendingInTransactions(transactionsBeingVerified, outputsToCheck);

        } else {
            //Also verify the current block that we are building for double spending.
            final var transactionsInMiningBlock = getCurrentBlock().getTransactionsCurrentlyInBlock();

            if (getCurrentBlock().hasTransaction(newTransaction.getTxID())) {
                System.out.println("Attempting to verify transaction that is already contained in the latest block.");
                return false;
            }

            if (!verifyDoubleSpendingInTransactions(transactionsInMiningBlock, outputsToCheck)) {
                return false;
            }
        }

        return true;
    }

    protected boolean isMinted(long blockNumber) {
        return this.blockCount > blockNumber;
    }

    protected boolean doesBlockExist(long blockNumber) {
        return this.blockCount >= blockNumber;
    }

    private Transaction verifyTransactionExistsInTransactions(ScriptSignature input,
                                                              LinkedHashMap<byte[], Transaction> transactions) {

        //Verify that the transaction that originated this input exists in the blockchain
        final var transactionByID = transactions.getOrDefault(input.getOriginatingTXID(), null);

        if (transactionByID == null) {
            return null;
        }

        //Verify that the transaction contains the output that generates the input
        if (!input.verifyOutputExists(transactionByID)) {
            System.out.println("Output does not exist");
            return null;
        }

        return transactionByID;
    }

    protected boolean verifyTransaction(Transaction transaction, long currentlyExaminingBlock,
                                        LinkedHashMap<byte[], Transaction> blockTransactions) {

        if (!transaction.verifyTransactionID()) {
            return false;
        }

        final var inputs = transaction.getInputs();

        float outputAmount = 0, inputAmount = 0;

        for (ScriptPubKey output : transaction.getOutputs()) {
            outputAmount += output.getAmount();
        }

        for (ScriptSignature input : inputs) {

            //Verify that the person that made the transaction has control over the private keys
            //That match the inputs and that the outputs have not been tampered with
            if (!input.verifyOwnership(transaction)) {
                return false;
            }

            Transaction parentT = null;

            if (doesBlockExist(input.getOriginatingBlock()) || (currentlyExaminingBlock == input.getOriginatingBlock())) {

                LinkedHashMap<byte[], Transaction> transactions;

                if (isMinted(input.getOriginatingBlock())) {
                    transactions = getBlockByNumber(input.getOriginatingBlock()).getTransactions();
                } else if (currentlyExaminingBlock == input.getOriginatingBlock()) {
                    transactions = blockTransactions;
                } else {
                    transactions = getCurrentBlock().getTransactionsCurrentlyInBlock();
                }

                parentT = verifyTransactionExistsInTransactions(input, transactions);

            } else {
                System.out.println("Block does not exist");
                return false;
            }

            if (parentT == null) {
                System.out.println("Failed to load the transactions whose outputs are used as inputs.");

                return false;
            }

            final var output = parentT.getOutputs()[input.getOutputIndex()];

            inputAmount += output.getAmount();
        }

        if (Float.compare(inputAmount, outputAmount) != 0) {
            System.out.println("The amounts do not match up!");

            return false;
        }

        //Since this is the most expensive part of verifying a transaction, only do it when everything else
        //Has already been verified
        if (!verifyDoubleSpending(transaction, currentlyExaminingBlock, blockTransactions)) {
            return false;
        }

        return true;
    }

    /**
     * Verifies that a transaction is valid according to this blockchain.
     * <p>
     * Checks for amount matching (Sum of inputs = Sum of outputs), verifies that the person making this transaction
     * Owns the outputs that it is using, verifies that the outputs that this transaction is referring to exist,
     * Verifies that the output haven't been spent already.
     *
     * @return
     */
    public boolean verifyTransaction(Transaction transaction) {
        //Passing -1 and null makes it check against the current block that's being built
        //When we are verifying external blocks, these arguments will be used to know what block is
        //Being checked
        return verifyTransaction(transaction, -1, null);
    }

}
