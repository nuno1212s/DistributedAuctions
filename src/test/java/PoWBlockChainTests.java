import me.evmanu.Standards;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.blocks.blockbuilders.BlockBuilder;
import me.evmanu.blockchain.blocks.blockbuilders.PoWBlockBuilder;
import me.evmanu.blockchain.blocks.blockchains.PoWBlockChain;
import me.evmanu.blockchain.transactions.ScriptPubKey;
import me.evmanu.blockchain.transactions.ScriptSignature;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.blockchain.transactions.TransactionType;
import me.evmanu.miner.MiningManager;
import me.evmanu.util.Hex;
import me.evmanu.util.Pair;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PoWBlockChainTests {

    private static final short version = (short) 0x01;

    private final PoWBlockChain blockChain = new PoWBlockChain(0, (short) 0x01, new ArrayList<>());

    public static Transaction initGenesisTransactionFor(float amountPerOutput, KeyPair... outputs) {

        final var keyGenerator = Standards.getKeyGenerator();

        assert keyGenerator != null;

        var output = new ScriptPubKey[outputs.length];

        for (int i = 0; i < outputs.length; i++) {
            var outputI = outputs[i];

            output[i] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(outputI.getPublic()), amountPerOutput);
        }

        return new Transaction(version, TransactionType.TRANSACTION, new ScriptSignature[0], output);
    }

    private Transaction initTransactionWithPreviousOutputs(Pair<Transaction, Integer>[] transactions,
                                                           Long[] transactionBlocks,
                                                           KeyPair[] correspondingKeys,
                                                           PublicKey[] outputs,
                                                           float[] amounts,
                                                           short version) throws IllegalAccessException {

        assert transactionBlocks.length == transactions.length;
        assert transactions.length == correspondingKeys.length;

        assert outputs.length == amounts.length;

        ScriptPubKey[] newOutputs = new ScriptPubKey[outputs.length];

        for (int i = 0; i < outputs.length; i++) {
            newOutputs[i] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(outputs[i]), amounts[i]);
        }

        ScriptSignature[] inputs = new ScriptSignature[transactions.length];

        for (int i = 0; i < transactions.length; i++) {

            final var transaction = transactions[i];
            inputs[i] = ScriptSignature.fromOutput(transaction.getKey(),
                    transactionBlocks[i],
                    transaction.getValue(), correspondingKeys[i], newOutputs);

        }

        return new Transaction(version, TransactionType.TRANSACTION, inputs, newOutputs);
    }

    @Test
    public void testGenesisThenUse() {
        final var currentBlock = blockChain.getCurrentBlock();

        final var keyGenerator = Standards.getKeyGenerator();

        assert keyGenerator != null;

        final var keyPair = keyGenerator.generateKeyPair();
        var keyPair2 = keyGenerator.generateKeyPair();

        var transaction = initGenesisTransactionFor(10, keyPair);

        currentBlock.addTransaction(transaction);

        System.out.println("Generated transaction: " + transaction);

        var outputs2 = new ScriptPubKey[2];

        var inputs2 = new ScriptSignature[1];

        outputs2[0] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(keyPair.getPublic()), 5);
        outputs2[1] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(keyPair2.getPublic()), 5);

        final ScriptSignature input1;

        try {
            input1 = ScriptSignature.fromOutput(transaction, currentBlock.getBlockNumber(), 0, keyPair, outputs2);

            inputs2[0] = input1;

            Transaction transaction1 = new Transaction(blockChain.getVersion(),
                    TransactionType.TRANSACTION,
                    inputs2, outputs2);

            System.out.println(transaction1);

            assert blockChain.verifyTransaction(transaction1);

            System.out.println("Added transaction.");
            currentBlock.addTransaction(transaction1);

        } catch (IllegalAccessException e) {
            e.printStackTrace();

            return;
        }
    }

    @Test
    public void testDoubleSpending() {

        try {
            final var currentBlock = blockChain.getCurrentBlock();

            final var keyGenerator = Standards.getKeyGenerator();

            assert keyGenerator != null;

            final var keyPair = keyGenerator.generateKeyPair();

            var keyPair2 = keyGenerator.generateKeyPair();

            var transaction = initGenesisTransactionFor(10, keyPair);

            currentBlock.addTransaction(transaction);

            System.out.println("Initial transaction: " + transaction);

            var inputs2 = new ScriptSignature[1];

            var outputs2 = new ScriptPubKey[1];

            outputs2[0] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(keyPair2.getPublic()), 10);

            inputs2[0] = ScriptSignature.fromOutput(transaction, currentBlock.getBlockNumber(), 0, keyPair, outputs2);

            Transaction transaction2 = new Transaction(blockChain.getVersion(),
                    TransactionType.TRANSACTION,
                    inputs2, outputs2);

            System.out.println("Transaction 2: " + transaction2);

            assert blockChain.verifyTransaction(transaction2);

            System.out.println("Added transaction.");
            currentBlock.addTransaction(transaction2);

            var inputs3 = new ScriptSignature[1];
            var outputs3 = new ScriptPubKey[2];

            outputs3[0] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(keyPair.getPublic()), 5);
            outputs3[1] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(keyPair2.getPublic()), 5);

            //Use the same input as the previous transaction
            inputs3[0] = ScriptSignature.fromOutput(transaction, currentBlock.getBlockNumber(), 0, keyPair, outputs3);

            Transaction transaction3 = new Transaction(blockChain.getVersion(),
                    TransactionType.TRANSACTION,
                    inputs3, outputs3);

            System.out.println("Transaction 3: " + transaction3);

            //Since we are double spending, we don't want the transaction to be verified
            assert !blockChain.verifyTransaction(transaction3);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInputOwnershipNotVerified() {
        final var currentBlock = blockChain.getCurrentBlock();

        var keyGenerator = Standards.getKeyGenerator();

        assert keyGenerator != null;

        var keyPair1 = keyGenerator.generateKeyPair();
        var keyPair2 = keyGenerator.generateKeyPair();

        final var transaction = initGenesisTransactionFor(10, keyPair1);

        currentBlock.addTransaction(transaction);

        var inputs = new ScriptSignature[1];

        var outputs = new ScriptPubKey[1];

        outputs[0] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(keyPair2.getPublic()), 10);

        try {
            inputs[0] = ScriptSignature.fromOutput(transaction, currentBlock.getBlockNumber(), 0, keyPair2, outputs);

            var wrongTransaction = new Transaction(blockChain.getVersion(),
                    TransactionType.TRANSACTION, inputs, outputs);

            assert !blockChain.verifyTransaction(wrongTransaction);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    //@Test
    @Ignore
    public void testBlockVerificationCorrectBlock() {
        final var keyGenerator = Standards.getKeyGenerator();

        assert keyGenerator != null;

        final var keyPair = keyGenerator.generateKeyPair();

        final var latestValidBlock = blockChain.getLatestValidBlock();

        byte[] prevBlockHash = new byte[0];

        if (latestValidBlock != null) {
            prevBlockHash = latestValidBlock.getHeader().getBlockHash();
        }

        BlockBuilder block = new PoWBlockBuilder(blockChain.getBlockCount(), blockChain.getVersion(), prevBlockHash);

        final var transaction = initGenesisTransactionFor(10, keyPair);

        final var transaction2 = initGenesisTransactionFor(20, keyPair);

        assert blockChain.verifyTransaction(transaction);

        assert blockChain.verifyTransaction(transaction2);

        block.addTransaction(transaction);

        block.addTransaction(transaction2);

        var builtBlock = block.build();

        assert blockChain.verifyBlock(builtBlock);

        blockChain.addBlock(builtBlock);
    }

    @Test
    public void testNewBuilder() {
        final PoWBlockChain blockChain = new PoWBlockChain(0, (short) 0x01, new ArrayList<>());

        final var keyGenerator = Standards.getKeyGenerator();

        assert keyGenerator != null;

        final var keyPair = keyGenerator.generateKeyPair();
        final var keyPair2 = keyGenerator.generateKeyPair();

        assert blockChain.getBlockCount() == 0;

        List<Transaction> transactions = new LinkedList<>();

        final var transaction = initGenesisTransactionFor(10, keyPair);

        final var transaction2 = initGenesisTransactionFor(20, keyPair2);

        transactions.add(transaction);
        transactions.add(transaction2);

        PoWBlockBuilder blockBuilder = PoWBlockBuilder.fromTransactionList(0, version, new byte[0], transactions);

        MiningManager manager = new MiningManager(null, blockChain);

        manager.assignWork(blockBuilder);
        manager.blockOnWorkers();

        assert blockChain.getBlockCount() > 0;

        Block latestValidBlock = blockChain.getLatestValidBlock();

        System.out.println("Latest block hash " + Hex.toHexString(latestValidBlock.getHeader().getBlockHash()));

        try {
            Transaction testTransaction = initTransactionWithPreviousOutputs(new Pair[]{
                            Pair.of(transaction, 0)
                    },
                    new Long[]{
                            blockBuilder.getBlockNumber()},

                    new KeyPair[]{
                            keyPair
                    },
                    new PublicKey[]{
                            keyPair.getPublic()
                    },
                    new float[]{
                            10
                    },
                    (short) 0x1);

            assert blockChain.verifyTransaction(testTransaction);

            transactions.clear();

            transactions.add(testTransaction);

            PoWBlockBuilder nBlockBuilder = PoWBlockBuilder.fromTransactionList(
                    latestValidBlock.getHeader().getBlockNumber() + 1,
                    version,
                    latestValidBlock.getHeader().getBlockHash(), transactions);

            manager.assignWork(nBlockBuilder);
            manager.blockOnWorkers();

            assert blockChain.getBlockCount() > 1;

            final var transaction3 = initGenesisTransactionFor(20, keyPair);

            assert !blockChain.verifyTransaction(transaction3);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

}
