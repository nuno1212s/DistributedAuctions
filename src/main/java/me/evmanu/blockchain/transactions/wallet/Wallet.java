package me.evmanu.blockchain.transactions.wallet;

import lombok.Getter;
import lombok.Setter;
import me.evmanu.Standards;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.transactions.ScriptPubKey;
import me.evmanu.blockchain.transactions.ScriptSignature;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.blockchain.transactions.TransactionType;
import me.evmanu.util.ByteWrapper;
import me.evmanu.util.Pair;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

@Getter
public class Wallet {

    private List<KeyPair> ListKeyPair = new ArrayList<KeyPair>();
    private Set<ByteWrapper> hashedListKeyPair = new ConcurrentSkipListSet<>();

    private final List<Transaction> ListTransactions = new ArrayList<Transaction>();
    private float balance;


    public Wallet() {
        //this.balance = getBalance();
        //this.hashedListKeyPair = hashOfPublicKeys();
    }

    //public Wallet() {
    //}

    private KeyPair generateKeyPair() {
        KeyPairGenerator kpg = Standards.getKeyGenerator(); // create object keyPairGenerator

        return kpg.generateKeyPair();
    }

    // add a individual keypair
    private void addNewKeyPair(KeyPair kp) {

        this.ListKeyPair.add(kp);

        this.hashedListKeyPair.add(new ByteWrapper(Standards.calculateHashedPublicKeyFrom(kp.getPublic())));
    }

    // Calculates the hashes of the keys pairs
    private Set<ByteWrapper> hashOfPublicKeys() {

        Set<ByteWrapper> publicHashKey = new TreeSet<>();

        for (KeyPair keyPair : this.ListKeyPair) {
            publicHashKey.add(new ByteWrapper(Standards.calculateHashedPublicKeyFrom(keyPair.getPublic())));
        }

        return publicHashKey;
    }

    private List<Transaction> getWalletTransactions(Block block) {

        List<Transaction> lstOfTransactions = new ArrayList<>();

        LinkedHashMap<ByteWrapper, Transaction>  transactions = block.getTransactions();

        for (Map.Entry<ByteWrapper, Transaction> transactionEntry : transactions.entrySet()) {

            for (ScriptPubKey output : transactionEntry.getValue().getOutputs()) {
                if (this.hashedListKeyPair.contains(new ByteWrapper(output.getHashedPubKey())))
                    lstOfTransactions.add(transactionEntry.getValue());
            }
        }

        return lstOfTransactions;
    }

    private float getBalance() {
        float total = 0;

        for (Transaction transaction : this.ListTransactions) {

            for (ScriptPubKey curScriptSignarure : transaction.getOutputs()) {

                if (this.hashedListKeyPair.contains(new ByteWrapper(curScriptSignarure.getHashedPubKey())))
                    total += curScriptSignarure.getAmount();
            }
        }

        return total;
    }

    private Transaction sendTransaction(Pair<Transaction, Integer>[] transactions,
                                        Long[] transactionBlocks,
                                        KeyPair[] correspondingKeys,
                                        PublicKey[] outputs,
                                        float[] amounts,
                                        short version,
                                        PublicKey changeKey) throws IllegalAccessException {

        float availableAmount = 0;
        float sumOfAmounts = 0;
        float change = 0;

        // Get the total available amount, to calculate the change
        for (final Pair<Transaction, Integer> transaction : transactions) {

            for (ScriptPubKey curScriptSPubKey : transaction.getKey().getOutputs()) {
                availableAmount += curScriptSPubKey.getAmount();
            }
        }

        // Total amount to be "removed" from the wallet
        for (float amount : amounts) {
            sumOfAmounts += amount;
        }

        change = availableAmount - sumOfAmounts;

        if(change < 0) System.out.println("Error! Change < 0!");

        ScriptPubKey[] newOutputs = new ScriptPubKey[outputs.length];

        for (int i = 0; i < outputs.length; i++) {
            newOutputs[i] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(outputs[i]), amounts[i]);
        }

        ScriptSignature[] newInputs = new ScriptSignature[transactions.length];

        for (int i = 0; i < transactions.length; i++) {

            final var transaction = transactions[i];
            newInputs[i] = ScriptSignature.fromOutput(transaction.getKey(),
                    transactionBlocks[i],
                    transaction.getValue(), correspondingKeys[i], newOutputs);

        }

        if(change > 0) {
            newOutputs[outputs.length] = new ScriptPubKey(Standards.calculateHashedPublicKeyFrom(changeKey), change);

            // TODO: INPUTS

            //newInputs[outputs.length] = ScriptSignature.fromOutput(transaction.getKey(),
            //        transactionBlocks[i],
            //        transaction.getValue(), correspondingKeys[i], newOutputs);
        }

        return new Transaction(version, TransactionType.TRANSACTION, newInputs, newOutputs);
    }

    public static void main(String Args[]) {

        Wallet mywallet = new Wallet();

        KeyPair kp1 = mywallet.generateKeyPair();
        KeyPair kp2 = mywallet.generateKeyPair();
        KeyPair kp3 = mywallet.generateKeyPair();

        mywallet.addNewKeyPair(kp1);
        mywallet.addNewKeyPair(kp2);
        mywallet.addNewKeyPair(kp3);

        System.out.println("test");

    }
}
