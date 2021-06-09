import me.evmanu.Standards;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.BlockChainHandler;
import me.evmanu.blockchain.blocks.blockbuilders.PoWBlockBuilder;
import me.evmanu.blockchain.blocks.blockchains.PoWBlockChain;
import me.evmanu.blockchain.transactions.ScriptPubKey;
import me.evmanu.blockchain.transactions.ScriptSignature;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.blockchain.transactions.TransactionType;
import me.evmanu.miner.MiningManager;
import me.evmanu.util.Hex;
import me.evmanu.util.Pair;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;

public class BlockChainHandlerTest {

    private static final short VERSION = 0x1;

    private Transaction initGenesisTransactionFor(float amountPerOutput, KeyPair... outputs) {

        final var keyGenerator = Standards.getKeyGenerator();

        assert keyGenerator != null;

        var output = new ScriptPubKey[outputs.length];

        for (int i = 0; i < outputs.length; i++) {
            var outputI = outputs[i];

            output[i] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(outputI.getPublic()), amountPerOutput);
        }

        return new Transaction(VERSION, TransactionType.TRANSACTION, new ScriptSignature[0], output);
    }

    private Transaction initTransactionWithPreviousOutputs(Pair<Transaction, Integer>[] transactions,
                                                           Long[] transactionBlocks,
                                                           KeyPair[] correspondingKeys,
                                                           PublicKey[] outputs,
                                                           float[] amounts) throws IllegalAccessException {

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

        return new Transaction(VERSION, TransactionType.TRANSACTION, inputs, newOutputs);
    }

    @Test
    public void testFork() {
        BlockChainHandler handler = new BlockChainHandler(new PoWBlockChain(0, VERSION, new ArrayList<>()));

        assert handler.getBestCurrentChain().isPresent();

        final var keyGenerator = Standards.getKeyGenerator();

        assert keyGenerator != null;

        final var keyPair = keyGenerator.generateKeyPair();
        final var keyPair2 = keyGenerator.generateKeyPair();

        List<Transaction> transactions = new LinkedList<>();

        final var transaction = initGenesisTransactionFor(10, keyPair);

        final var transaction2 = initGenesisTransactionFor(20, keyPair2);

        transactions.add(transaction);
        transactions.add(transaction2);

        MiningManager manager = new MiningManager(null, handler);

        PoWBlockBuilder blockBuilder = PoWBlockBuilder.fromTransactionList(0, VERSION, new byte[0], transactions);

        manager.assignWork(blockBuilder);
        manager.blockOnWorkers();

        assert handler.getBestCurrentChain().get().getBlockCount() > 0;

        transactions.clear();

        try {
            Transaction nTransaction1 = initTransactionWithPreviousOutputs(new Pair[]{
                            Pair.of(transaction, 0)
                    },
                    new Long[]{0L},
                    new KeyPair[]{
                            keyPair
                    },
                    new PublicKey[]{
                            keyPair2.getPublic()
                    },
                    new float[]{10});

            assert handler.getBestCurrentChain().get().verifyTransaction(nTransaction1);

            transactions.add(nTransaction1);

            Transaction nTransaction2 = initTransactionWithPreviousOutputs(new Pair[]{
                            Pair.of(transaction2, 0)
                    },
                    new Long[]{0L},
                    new KeyPair[]{
                            keyPair2
                    },
                    new PublicKey[]{
                            keyPair.getPublic()
                    },
                    new float[]{20});

            assert handler.getBestCurrentChain().get().verifyTransaction(nTransaction2);

            transactions.add(nTransaction2);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        Block latestValidBlock = handler.getBestCurrentChain().get().getLatestValidBlock();

        PoWBlockBuilder secondBlock = PoWBlockBuilder.fromTransactionList(latestValidBlock.getHeader().getBlockNumber() + 1,
                VERSION, latestValidBlock.getHeader().getBlockHash(),
                transactions);

        manager.assignWork(secondBlock);
        manager.blockOnWorkers();

        Collections.reverse(transactions);

        PoWBlockBuilder repeatSecondBlock = PoWBlockBuilder.fromTransactionList(latestValidBlock.getHeader().getBlockNumber() + 1,
                VERSION, latestValidBlock.getHeader().getBlockHash(),
                transactions);

        manager.assignWork(repeatSecondBlock);
        manager.blockOnWorkers();

        //Now there should be 2 forked chains, both with the same size.

        assert handler.getCurrentBlockChains() == 2;

        transactions.clear();

        latestValidBlock = handler.getBestCurrentChain().get().getLatestValidBlock();

        //We will now build another block on the second chain, making it the most

        PoWBlockBuilder thirdBlock = PoWBlockBuilder.fromTransactionList(latestValidBlock.getHeader().getBlockNumber() + 1,
                VERSION, latestValidBlock.getHeader().getBlockHash(),
                transactions);

        manager.assignWork(thirdBlock);
        manager.blockOnWorkers();

        latestValidBlock = handler.getBestCurrentChain().get().getLatestValidBlock();

        assert latestValidBlock.getHeader().getBlockNumber() == thirdBlock.getBlockNumber();

        PoWBlockBuilder thirdBlockRepeat = PoWBlockBuilder.fromTransactionList(thirdBlock.getBlockNumber(),
                VERSION, thirdBlock.getPreviousBlockHash(),
                transactions);

        manager.assignWork(thirdBlockRepeat);
        manager.blockOnWorkers();

        assert handler.getCurrentBlockChains() == 3;
        assert handler.getBestCurrentChain().get().getBlockCount() == 3;

        System.out.println("Latest block " + Hex.toHexString(latestValidBlock.getHeader().getBlockHash()));
    }


}
