package me.evmanu.blockchain.blocks.blockchains;

import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.blocks.BlockChain;
import me.evmanu.blockchain.blocks.BlockChainStandards;
import me.evmanu.blockchain.blocks.BlockHeader;
import me.evmanu.blockchain.transactions.ScriptPubKey;
import me.evmanu.blockchain.transactions.ScriptSignature;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.blockchain.transactions.TransactionType;
import me.evmanu.util.ByteWrapper;
import me.evmanu.util.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class PoSBlockChain extends BlockChain {

    /**
     * A record of all the stakes made and the block where they were
     * withdrawn (if that is the case)
     */
    private final Map<ByteWrapper, Pair<Transaction, Long>> allTimeStakes;

    public PoSBlockChain(long blockCount, short version, List<Block> blocks) {
        super(blockCount, version, blocks);

        this.allTimeStakes = new ConcurrentSkipListMap<>();

        scanAllBlocksForStakes();
    }

    private void scanAllBlocksForStakes() {
        for (Block block : this.blocks) {
            PoSBlock poSBlock = (PoSBlock) block;

            scanBlockForStakes(poSBlock);
        }
    }

    private void scanBlockForStakes(PoSBlock block) {

        for (Map.Entry<ByteWrapper, Transaction> transaction : block.getTransactions().entrySet()) {

            if (isTransactionAStake(transaction.getValue())) {
                allTimeStakes.put(transaction.getKey(), Pair.of(transaction.getValue(), -1L));
            } else if (isTransactionAStakeWithdrawal(transaction.getValue())) {

                Pair<Transaction, Long> transactionLongPair = allTimeStakes.get(transaction.getKey());

                if (transactionLongPair == null) {
                    throw new IllegalArgumentException("Cannot withdraw a transaction without it first being deposited");
                }

                transactionLongPair.setValue(block.getHeader().getBlockNumber());
            }
        }
    }

    /**
     * A transaction that is a stake has the following format:
     * 1 input with the stake amount,
     * 0 outputs.
     *
     * @param transaction
     * @return
     */
    public boolean isTransactionAStake(Transaction transaction) {

        if (transaction.getInputs().length == 1 && transaction.getOutputs().length == 0 && transaction.getType() == TransactionType.STAKE) {

            ScriptSignature input = transaction.getInputs()[0];

            Block blockByNumber = this.getBlockByNumber(input.getOriginatingBlock());

            //We don't have to verify the originating transaction as since this transaction is already in the block
            //Chain
            Transaction originatingTransaction = blockByNumber.getTransactionByID(input.getOriginatingTXID());

            if (originatingTransaction == null) return false;

            if (!input.verifyOutputExists(originatingTransaction)) {
                return false;
            }

            ScriptPubKey output = originatingTransaction.getOutputs()[input.getOutputIndex()];

            return Float.compare(output.getAmount(), BlockChainStandards.STAKE_AMOUNT) == 0;
        }

        return false;
    }

    /**
     * Is the given transaction a withdrawal from the stake
     *
     * @param transaction
     * @return
     */
    public boolean isTransactionAStakeWithdrawal(Transaction transaction) {

        if (transaction.getInputs().length == 1 && transaction.getOutputs().length == 1
                && transaction.getType() == TransactionType.STAKE_WITHDRAWAL) {

            ScriptSignature input = transaction.getInputs()[0];

            Block blockByNumber = getBlockByNumber(input.getOriginatingBlock());

            Transaction originalStake = blockByNumber.getTransactionByID(input.getOriginatingTXID());

            ScriptSignature stake = originalStake.getInputs()[0];

            if (isTransactionAStake(originalStake)) {

                ScriptSignature stakeInput = stake;

                //To withdraw a stake, we must have as an input the same public key that was used to
                //Input the stake
                if (!Arrays.equals(stakeInput.getPublicKey(), input.getPublicKey())) return false;

            } else {
                return false;
            }

            if (!input.verifyOwnership(transaction)) {
                return false;
            }

            Block block2 = getBlockByNumber(stake.getOriginatingBlock());

            Transaction tx = block2.getTransactionByID(stake.getOriginatingTXID());

            //The output that originated the stake must equal the output that the user is
            //Trying to withdraw
            ScriptPubKey stakeOutput = tx.getOutputs()[stake.getOutputIndex()];

            ScriptPubKey output = transaction.getOutputs()[0];

            if (Float.compare(output.getAmount(), stakeOutput.getAmount()) != 0) return false;

            //If the originating transaction is a stake, the input given here corresponds to it and the amounts
            //Match up, then we have a successful iteration

            return true;
        }

        return false;
    }

    public List<Transaction> getAllowedSignersForBlock(long blockNumber) {

        if (!isMinted(blockNumber - 1)) {

            System.out.println("Can only get the signers for up to a block in succession");

            return null;
        }

        Block blockByNumber = getBlockByNumber(blockNumber - 1);

        byte[] blockHash = blockByNumber.getHeader().getBlockHash();

        long initialSeed = 0;

        for (int i = 0; i < blockHash.length; i++) {

            int shift = Byte.SIZE * (i % Long.BYTES);

            initialSeed ^= ((long) blockHash[i] << shift);
        }

        List<Transaction> activeStakes = new ArrayList<>(this.allTimeStakes.size());

        for (Map.Entry<ByteWrapper, Pair<Transaction, Long>> stake : this.allTimeStakes.entrySet()) {

            Long withdrawal = stake.getValue().getValue();

            if (withdrawal == null || withdrawal == -1) {
                activeStakes.add(stake.getValue().getKey());
            } else {

                if (withdrawal <= blockNumber) {
                    continue;
                }

                activeStakes.add(stake.getValue().getKey());
            }
        }

        List<Transaction> allowedSigners = new ArrayList<>(BlockChainStandards.ALLOWED_SIGNERS);

        Random random = new Random(initialSeed);

        for (int i = 0; i < BlockChainStandards.ALLOWED_SIGNERS && !activeStakes.isEmpty(); i++) {
            Transaction chosen = activeStakes.remove(random.nextInt(activeStakes.size()));

            allowedSigners.add(chosen);
        }

        return allowedSigners;
    }

    public Optional<Transaction> getActiveStake(byte[] transactionID, long verifyingBlock) {

        Pair<Transaction, Long> transactions = this.allTimeStakes.get(new ByteWrapper(transactionID));

        if (transactions == null) return Optional.empty();

        if (transactions.getValue() != null && transactions.getValue() != -1) {
            if (transactions.getValue() <= verifyingBlock) {
                //If the stake was removed before the block that is being verified, then it can't be a correct
                //verification
                return Optional.empty();
            }
        }

        return Optional.of(transactions.getKey());
    }

    @Override
    public synchronized Optional<BlockChain> addBlock(Block block) {

        Optional<BlockChain> forked = super.addBlock(block);

        if (forked.isEmpty()) {
            scanBlockForStakes((PoSBlock) block);
        }

        return forked;
    }

    @Override
    public boolean verifyBlock(Block block) {

        if (!(block instanceof PoSBlock)) {
            System.out.println("PoS block chain requires PoS blocks!");

            return false;
        }

        if (!block.verifyBlockID()) {
            System.out.println("The block hash does not match the content of the block.");
            return false;
        }

        if (!block.isValid(this)) {
            return false;
        }

        BlockHeader header = block.getHeader();

        long blockNumber = header.getBlockNumber();

        Block prevBlock = null;

        if (blockNumber > 0) {

            if (blockNumber <= getBlockCount() && !canForkAt(blockNumber)) {

                System.out.println("This block chain is already further ahead of the block received.");

                return false;
            }

            prevBlock = getBlockByNumber(blockNumber - 1);
        }

        if (!block.verifyPreviousBlockHash(prevBlock)) {
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
    public BlockChain fork(long blockForkNumber) {

        List<Block> previousBlocks = new ArrayList<>((int) (blockForkNumber + 1));

        for (long block = 0; block < blockForkNumber; block++) {
            //We want to maintain the pointer to the previous blocks,
            //Not copy them, as they are exactly the same, only the following blocks might
            //Be different
            previousBlocks.add(getBlockByNumber(block));
        }

        return new PoSBlockChain(previousBlocks.size(), getVersion(), previousBlocks);
    }
}
