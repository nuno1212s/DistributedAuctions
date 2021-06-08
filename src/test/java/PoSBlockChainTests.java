import me.evmanu.Standards;
import me.evmanu.blockchain.Signable;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.blocks.BlockChainStandards;
import me.evmanu.blockchain.blocks.blockbuilders.PoSBlockBuilder;
import me.evmanu.blockchain.blocks.blockchains.PoSBlockChain;
import me.evmanu.blockchain.transactions.ScriptPubKey;
import me.evmanu.blockchain.transactions.ScriptSignature;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.blockchain.transactions.TransactionType;
import me.evmanu.util.Hex;
import me.evmanu.util.Pair;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PoSBlockChainTests {

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
                                                           float[] amounts,
                                                           short version) throws IllegalAccessException {

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

    private Transaction initStakeTransactionFor(Long[] blockNumbers, Transaction[] inputs, KeyPair[] keyPairs) {

        var output = new ScriptPubKey[0];

        int outputs = 0;

        for (int i = 0; i < inputs.length; i++) {

            outputs += inputs[i].getOutputs().length;

        }

        var signatures = new ScriptSignature[outputs];

        for (int i = 0; i < inputs.length; i++) {

            var transaction = inputs[i];

            for (int j = 0; j < transaction.getOutputs().length; j++) {
                ScriptSignature input = null;

                try {
                    input = ScriptSignature.fromOutput(
                            transaction,
                            blockNumbers[i],
                            j,
                            keyPairs[i],
                            output
                    );
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                signatures[i] = input;
            }
        }

        return new Transaction(VERSION, TransactionType.STAKE, signatures, output);
    }

    @Test
    public void testFirstBlockStakeThenSign() {

        PoSBlockChain blockChain = new PoSBlockChain(0, (short) 0x1, new ArrayList<>());

        List<Transaction> transactions = new LinkedList<>();

        var generator = Standards.getKeyGenerator();

        var keyPair = generator.generateKeyPair();
        var keyPair2 = generator.generateKeyPair();

        var transaction1 = initGenesisTransactionFor(BlockChainStandards.STAKE_AMOUNT, keyPair);

        var transaction2 = initGenesisTransactionFor(BlockChainStandards.STAKE_AMOUNT, keyPair2);

        var stake = initStakeTransactionFor(new Long[]{
                        0L
                }, new Transaction[]{
                        transaction1
                },
                new KeyPair[]{
                        keyPair
                });

        transactions.add(transaction1);
        transactions.add(transaction2);
        transactions.add(stake);

        var poSBlockBuilder = PoSBlockBuilder.fromTransactionList(0, VERSION, new byte[0], transactions);

        poSBlockBuilder.setSigningID(new byte[0]);
        poSBlockBuilder.setSignature(new byte[0]);

        assert blockChain.getBlockCount() == 0;

        blockChain.addBlock(poSBlockBuilder.build());

        transactions.clear();

        assert blockChain.getBlockCount() > 0;

        var latestValidBlock = blockChain.getLatestValidBlock();

        System.out.println(transaction1);
        System.out.println(transaction2);

        try {
            Transaction repeatUse = initTransactionWithPreviousOutputs(
                    new Pair[]{
                            Pair.of(transaction1, 0)
                    },
                    new Long[]{
                            latestValidBlock.getHeader().getBlockNumber()
                    },
                    new KeyPair[]{
                            keyPair
                    },
                    new PublicKey[]{
                            keyPair.getPublic()
                    },
                    new float[]{
                            BlockChainStandards.STAKE_AMOUNT
                    },
                    VERSION
            );

            assert !blockChain.verifyTransaction(repeatUse);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }


        try {
            Transaction repeatUse = initTransactionWithPreviousOutputs(
                    new Pair[]{
                            Pair.of(transaction2, 0)
                    },
                    new Long[]{
                            latestValidBlock.getHeader().getBlockNumber()
                    },
                    new KeyPair[]{
                            keyPair2
                    },
                    new PublicKey[]{
                            keyPair2.getPublic()
                    },
                    new float[]{
                            BlockChainStandards.STAKE_AMOUNT
                    },
                    VERSION
            );

            assert blockChain.verifyTransaction(repeatUse);

            transactions.add(repeatUse);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        var blockBuilder2 = PoSBlockBuilder.fromTransactionList(blockChain.getBlockCount(),
                VERSION, latestValidBlock.getHeader().getBlockHash(), transactions);

        blockBuilder2.setSigningID(stake.getTxID());

        blockBuilder2.setSignature(Signable.calculateSignatureOf(blockBuilder2, keyPair.getPrivate()));

        var block2 = blockBuilder2.build();

        System.out.println("Public key here: " + Hex.toHexString(keyPair.getPublic().getEncoded()));

        assert blockChain.verifyBlock(block2);

        blockChain.addBlock(block2);

        assert blockChain.getBlockCount() > 1;
    }

    @Test
    public void testFirstBlockDoubleStakeThenSign() {

        PoSBlockChain blockChain = new PoSBlockChain(0, (short) 0x1, new ArrayList<>());

        List<Transaction> transactions = new LinkedList<>();

        var generator = Standards.getKeyGenerator();

        var keyPair = generator.generateKeyPair();
        var keyPair2 = generator.generateKeyPair();

        var transaction1 = initGenesisTransactionFor(BlockChainStandards.STAKE_AMOUNT, keyPair);

        var transaction2 = initGenesisTransactionFor(BlockChainStandards.STAKE_AMOUNT, keyPair2);

        var stake = initStakeTransactionFor(new Long[]{
                        0L
                }, new Transaction[]{
                        transaction1
                },
                new KeyPair[]{
                        keyPair
                });

        transactions.add(transaction1);
        transactions.add(transaction2);
        transactions.add(stake);

        var poSBlockBuilder = PoSBlockBuilder.fromTransactionList(0, VERSION, new byte[0], transactions);

        poSBlockBuilder.setSigningID(new byte[0]);
        poSBlockBuilder.setSignature(new byte[0]);

        assert blockChain.getBlockCount() == 0;

        blockChain.addBlock(poSBlockBuilder.build());

        transactions.clear();

        assert blockChain.getBlockCount() > 0;

        var latestValidBlock = blockChain.getLatestValidBlock();

        System.out.println(transaction1);
        System.out.println(transaction2);

        try {
            Transaction repeatUse = initTransactionWithPreviousOutputs(
                    new Pair[]{
                            Pair.of(transaction1, 0)
                    },
                    new Long[]{
                            latestValidBlock.getHeader().getBlockNumber()
                    },
                    new KeyPair[]{
                            keyPair
                    },
                    new PublicKey[]{
                            keyPair.getPublic()
                    },
                    new float[]{
                            BlockChainStandards.STAKE_AMOUNT
                    },
                    VERSION
            );

            assert !blockChain.verifyTransaction(repeatUse);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        var stake2 = initStakeTransactionFor(new Long[]{
                        latestValidBlock.getHeader().getBlockNumber()
                },
                new Transaction[]{
                        transaction2
                },
                new KeyPair[]{
                        keyPair2
                });

        assert blockChain.verifyTransaction(stake2);

        transactions.add(stake2);

        var blockBuilder2 = PoSBlockBuilder.fromTransactionList(blockChain.getBlockCount(),
                VERSION, latestValidBlock.getHeader().getBlockHash(), transactions);

        blockBuilder2.setSigningID(stake.getTxID());

        blockBuilder2.setSignature(Signable.calculateSignatureOf(blockBuilder2, keyPair.getPrivate()));

        var block2 = blockBuilder2.build();

        System.out.println("Public key here: " + Hex.toHexString(keyPair.getPublic().getEncoded()));

        assert blockChain.verifyBlock(block2);

        blockChain.addBlock(block2);

        assert blockChain.getBlockCount() > 1;

        List<Transaction> allowedSignersForBlock = blockChain.getAllowedSignersForBlock(blockBuilder2.getBlockNumber() + 1);

        System.out.println(allowedSignersForBlock);

        assert allowedSignersForBlock.size() == 2;

        var stake3 = initStakeTransactionFor(new Long[]{
                        latestValidBlock.getHeader().getBlockNumber()
                },
                new Transaction[]{
                        transaction2
                },
                new KeyPair[]{
                        keyPair2
                });

        assert !blockChain.verifyTransaction(stake3);
    }

}
