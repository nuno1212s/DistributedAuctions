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
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.Signature;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PoSBlockChainTests {

    private static final short VERSION = 0x1;

    PoSBlockChain blockChain = new PoSBlockChain(0, (short) 0x1, new ArrayList<>());

    private Transaction initGenesisTransactionFor(float amountPerOutput, KeyPair... outputs) {

        final var keyGenerator = Standards.getKeyGenerator();

        assert keyGenerator != null;

        var output = new ScriptPubKey[outputs.length];

        for (int i = 0; i < outputs.length; i++) {
            var outputI = outputs[i];

            output[i] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(outputI.getPublic()), amountPerOutput);
        }

        return new Transaction(blockChain.getVersion(), TransactionType.TRANSACTION, new ScriptSignature[0], output);
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

        return new Transaction(blockChain.getVersion(), TransactionType.STAKE, signatures, output);
    }

    @Test
    public void testFirstBlockStakeThenSign() {

        List<Transaction> transactions = new LinkedList<>();

        var generator = Standards.getKeyGenerator();

        var keyPair = generator.generateKeyPair();

        var transaction1 = initGenesisTransactionFor(BlockChainStandards.STAKE_AMOUNT, keyPair);

        var transaction2 = initGenesisTransactionFor(BlockChainStandards.STAKE_AMOUNT, keyPair);

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

}
