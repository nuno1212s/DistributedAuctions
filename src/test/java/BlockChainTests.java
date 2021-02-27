import me.evmanu.Standards;
import me.evmanu.daos.blocks.BlockChain;
import me.evmanu.daos.transactions.ScriptPubKey;
import me.evmanu.daos.transactions.ScriptSignature;
import me.evmanu.daos.transactions.Transaction;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.util.ArrayList;

public class BlockChainTests {

    private BlockChain blockChain = new BlockChain(0, (short) 0x01, new ArrayList<>());

    @Test
    public void testGenesisThenUse() {
        final var currentBlock = blockChain.getCurrentBlock();

        final var keyGenerator = Standards.getKeyGenerator();

        assert keyGenerator != null;

        final var keyPair = keyGenerator.generateKeyPair();
        var keyPair2 = keyGenerator.generateKeyPair();

        var output = new ScriptPubKey[1];

        output[0] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(keyPair.getPublic()), 10);

        Transaction transaction = new Transaction(currentBlock.getBlock_number(), blockChain.getVersion(),
                new ScriptSignature[0], output);

        currentBlock.addTransaction(transaction);

        System.out.println("Generated transaction: " + transaction);

        var outputs2 = new ScriptPubKey[2];

        var inputs2 = new ScriptSignature[1];

        outputs2[0] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(keyPair.getPublic()), 5);
        outputs2[1] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(keyPair2.getPublic()), 5);

        final ScriptSignature input1;

        try {
            input1 = ScriptSignature.fromOutput(transaction, 0, keyPair, outputs2);

            inputs2[0] = input1;

            Transaction transaction1 = new Transaction(currentBlock.getBlock_number(), blockChain.getVersion(),
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

            var output = new ScriptPubKey[1];

            output[0] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(keyPair.getPublic()), 10);

            Transaction transaction = new Transaction(currentBlock.getBlock_number(), blockChain.getVersion(),
                    new ScriptSignature[0], output);

            currentBlock.addTransaction(transaction);

            System.out.println("Initial transaction: " + transaction);

            var inputs2 = new ScriptSignature[1];

            var outputs2 = new ScriptPubKey[1];

            outputs2[0] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(keyPair2.getPublic()), 10);

            inputs2[0] = ScriptSignature.fromOutput(transaction, 0, keyPair, outputs2);

            Transaction transaction2 = new Transaction(currentBlock.getBlock_number(), blockChain.getVersion(),
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
            inputs3[0] = ScriptSignature.fromOutput(transaction, 0, keyPair, outputs3);

            Transaction transaction3 = new Transaction(currentBlock.getBlock_number(), blockChain.getVersion(),
                    inputs3, outputs3);

            System.out.println("Transaction 3: " + transaction3);

            assert !blockChain.verifyTransaction(transaction3);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
