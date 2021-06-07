package me.evmanu.blockchain.blocks;

import lombok.Getter;
import me.evmanu.blockchain.blocks.blockbuilders.BlockBuilder;
import me.evmanu.blockchain.blocks.blockbuilders.PoWBlockBuilder;
import me.evmanu.blockchain.transactions.ScriptPubKey;
import me.evmanu.blockchain.transactions.ScriptSignature;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.blockchain.transactions.TransactionType;
import me.evmanu.util.ByteWrapper;
import me.evmanu.util.Hex;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public abstract class BlockChain {

    /**
     * The amount of blocks currently in this block chain.
     */
    protected AtomicLong blockCount;

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
        this.blockCount = new AtomicLong(blockCount);
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
        if (this.blockCount.get() == 0) return null;

        return getBlockByNumber(this.blockCount.get() - 1);
    }

    public Block getBlockByNumber(long blockNumber) {
        //For this assignment, I think we can allow for a limit of 2^32 blocks.
        //But, TODO: Change this to allow for larger block sizes
        return blocks.get(((Long) blockNumber).intValue());
    }

    public long getBlockCount() {
        return this.blockCount.get();
    }

    /**
     * Add a block to the block chain
     * <p>
     * This assumes that the block has already been verified
     *
     * @param block
     */
    public synchronized Optional<BlockChain> addBlock(Block block) {

        BlockHeader header = block.getHeader();

        long blockNum = header.getBlockNumber();

        if (blockNum > 0) {

            if (blockNum <= getBlockCount() && !canForkAt(blockNum)) {

            } else {
                BlockChain fork = this.fork(blockNum - 1);

                fork.addBlock(block);

                return Optional.of(fork);
            }
        }

        this.blocks.add(block);

        long count = this.blockCount.incrementAndGet();

        var previousBlock = this.currentBlock;

        this.currentBlock = new PoWBlockBuilder(count, this.version, block.getHeader().getBlockHash());

        for (Map.Entry<ByteWrapper, Transaction> transactions : previousBlock.getTransactionsCurrentlyInBlock().entrySet()) {

            if (block.verifyTransactionIsInBlock(transactions.getKey().getBytes())) {
                //The transaction has already been included into the newly generated block
                continue;
            }

            if (!verifyTransaction(transactions.getValue())) {
                //This transaction is no longer valid after the new block that we have received

                System.out.println("Transaction is no longer valid after new block has been mined");
                continue;
            }

            //If the transaction was not included in the most recent block and is still
            //valid with all the new transactions that have been included in the block
            //Then we still want to include it into the block that we are currently calculating
            this.currentBlock.addTransaction(transactions.getValue());
        }

        return Optional.empty();
    }

    /**
     * Verify a new incoming block against our own block chain, to assert that it's valid and can be integrated into our block
     * chain.
     * If this block is not deemed valid, we will broadcast a reject message for it.
     *
     * @param block
     * @return
     */
    public abstract boolean verifyBlock(Block block);

    /**
     * Verify that a list of outputs have not been spent in a list of transactions (Usually a block)
     *
     * @param transactions
     * @param outputsToCheck
     * @return
     */
    private boolean verifyDoubleSpendingInTransactions(LinkedHashMap<ByteWrapper, Transaction> transactions,
                                                       Map<ByteWrapper, List<Integer>> outputsToCheck) {

        for (Map.Entry<ByteWrapper, Transaction> transaction : transactions.entrySet()) {

            var trans = transaction.getValue();

            for (ScriptSignature input : trans.getInputs()) {

                ByteWrapper wrappedOriginatingTXID = new ByteWrapper(input.getOriginatingTXID());

                if (outputsToCheck.containsKey(wrappedOriginatingTXID)) {
                    var outputsTryingToSpend = outputsToCheck.get(wrappedOriginatingTXID);

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
                                         LinkedHashMap<ByteWrapper, Transaction> transactionsBeingVerified) {

        long oldestOriginatingBlock = 0;

        Map<ByteWrapper, List<Integer>> outputsToCheck = new HashMap<>();

        for (ScriptSignature input : newTransaction.getInputs()) {

            var originatingTXID = new ByteWrapper(input.getOriginatingTXID());

            var outputsForTrans = outputsToCheck.getOrDefault(originatingTXID, new LinkedList<>());

            outputsForTrans.add(input.getOutputIndex());

            outputsToCheck.put(originatingTXID, outputsForTrans);

            if (oldestOriginatingBlock < input.getOriginatingBlock()) {
                oldestOriginatingBlock = input.getOriginatingBlock();
            }
        }

        for (; oldestOriginatingBlock < blockCount.get(); oldestOriginatingBlock++) {
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
        return this.blockCount.get() > blockNumber;
    }

    protected boolean doesBlockExist(long blockNumber) {
        return this.blockCount.get() >= blockNumber;
    }

    private Transaction verifyTransactionExistsInTransactions(ScriptSignature input,
                                                              LinkedHashMap<ByteWrapper, Transaction> transactions) {

        //Verify that the transaction that originated this input exists in the blockchain
        final var transactionByID = transactions.getOrDefault(new ByteWrapper(input.getOriginatingTXID()), null);

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
                                        LinkedHashMap<ByteWrapper, Transaction> blockTransactions) {

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

                LinkedHashMap<ByteWrapper, Transaction> transactions;

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

    /**
     * Check if a certain transaction is the mining reward of a given block
     * @param block
     * @param transaction
     * @return
     */
    public boolean isMiningRewardTransaction(Block block, Transaction transaction) {

        if (transaction.getType() != TransactionType.MINING_REWARD) return false;

        Set<ByteWrapper> byteWrappers = block.getTransactions().keySet();

        Optional<ByteWrapper> first = byteWrappers.stream().findFirst();

        if (!first.isPresent()) return false;

        //The transaction has to be the first transaction in the block
        if (!Arrays.equals(transaction.getTxID(), first.get().getBytes())) {
            return false;
        }

        return transaction.getInputs().length == 0 && transaction.getOutputs().length == 1;
    }

    /**
     * Check if this block chain can be forked at a certain block
     *
     * @param blockNumber
     * @return
     */
    public boolean canForkAt(long blockNumber) {

        if (blockNumber < (this.blockCount.get() - BlockChainStandards.MAX_FORK_DISTANCE)) {
            //We cannot fork if there are already MAX_FORK_DISTANCE built blocks ahead of
            //the block that we are trying to fork at
            return false;
        }

        return true;
    }

    /**
     * Fork the block chain at a certain block, inclusive of the blockForkNumber
     *
     * @param blockForkNumber
     * @return
     */
    public abstract BlockChain fork(long blockForkNumber);

}
