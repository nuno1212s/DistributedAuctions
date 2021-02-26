package me.evmanu.daos.transactions;

import lombok.Getter;
import me.evmanu.daos.Hashable;

import java.nio.ByteBuffer;
import java.security.*;
import java.util.Arrays;
import java.util.Collection;

/**
 * Transaction element that is thread-safe as it is immutable (Same as {@link me.evmanu.daos.blocks.Block}
 */
@Getter
public class Transaction implements Hashable {

    private final byte[] txID;

    private final long blockNumber;

    private final short version;

    private final ScriptSignature[] inputs;

    private final ScriptPubKey[] outputs;

    public Transaction(int blockNumber, short version, ScriptSignature[] inputs, ScriptPubKey[] outputs) {
        this.version = version;
        this.blockNumber = blockNumber;
        this.inputs = inputs;
        this.outputs = outputs;

        this.txID = calculateTXIDFor(this);
    }

    public boolean verifyTransactionID() {
        return Arrays.equals(txID, calculateTXIDFor(this));
    }

    /**
     * Verifies that the output amounts match the input amounts, given the transactions that contain the outputs that correspond
     * To the inputs in this transaction
     *
     * This method does not check for double spending nor does it validate the inputTransactions
     *
     * @param inputTransactions
     * @return
     */
    public boolean verifyTransactionValidityAndAmounts(Collection<Transaction> inputTransactions) {

        if (!verifyTransactionID()) {
            return false;
        }

        float outputAmount = 0, inputAmount = 0;

        for (ScriptPubKey output : this.outputs) {
            outputAmount += output.getAmount();
        }

        for (ScriptSignature input : this.inputs) {

            var originatingTXID = input.getOriginatingTXID();

            var foundInput = false;

            Transaction originatingTransaction = null;

            for (Transaction inputTransaction : inputTransactions) {

                if (Arrays.equals(inputTransaction.getTxID(), originatingTXID)) {
                    if (!input.verifyOutputExists(inputTransaction)) {

                        System.out.println("The output that corresponds to the input " + input + " does not match!");

                        return false;
                    }

                    originatingTransaction = inputTransaction;

                    foundInput = true;
                    break;
                }
            }

            if (!foundInput) {
                System.out.println("Did not receive the transaction that originated the input " + input);
                return false;
            }

            final var output = originatingTransaction.getOutputs()[input.getOutputIndex()];

            inputAmount += output.getAmount();
        }

        if (Float.compare(inputAmount, outputAmount) != 0) {
            System.out.println("The amounts do not add up!");

            return false;
        }

        return true;
    }

    @Override
    public void addToHash(MessageDigest hash) {

        var buffer = ByteBuffer.allocate(Short.BYTES + Long.BYTES);

        buffer.putShort(version);
        buffer.putLong(blockNumber);

        hash.update(buffer.array());

        for (ScriptSignature input : inputs) {
            input.addToHash(hash);
        }

        for (ScriptPubKey output : outputs) {
            output.addToHash(hash);
        }
    }

    public static byte[] calculateTXIDFor(Transaction transaction) {
        return Hashable.calculateHashOf(transaction);
    }

}
