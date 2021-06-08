package me.evmanu.daos.transactions.wallet;
import me.evmanu.Standards;
import me.evmanu.auctions.Auction;
import me.evmanu.daos.transactions.ScriptPubKey;
import me.evmanu.daos.transactions.Transaction;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.List;
import java.security.*;

public class Wallet {

    List<KeyPair> ListKeyPair = new ArrayList<KeyPair>();
    List<Transaction> ListTransactions = new ArrayList<Transaction>();
    float balance;

    public Wallet() {
        generateKeyPair();
        this.balance = getBalance();
    }

    private KeyPair generateKeyPair() {
        KeyPairGenerator kpg = Standards.getKeyGenerator(); // create object keyPairGenerator

        return kpg.generateKeyPair();
    }


    private float getBalance() {
        float total = 0;

        List<byte[]> publicHashKey = hashOfPublicKeys();

        for (Transaction transaction : this.ListTransactions) {

            for (ScriptPubKey curScriptSignarure : transaction.getOutputs()) {

                if ( publicHashKey.contains(curScriptSignarure.getHashedPubKey()) )
                    total += curScriptSignarure.getAmount();
            }
        }

        return  total;
    }

    private Set<ByteWrapper> hashOfPublicKeys() {

        List<byte[]> publicHashKey = new ;

        for (KeyPair keyPair : this.ListKeyPair) {
            publicHashKey.add(Standards.calculateHashedPublicKeyFrom(keyPair.getPublic()));
        }

        return publicHashKey;
    }

    public static void main(String Args[]) {
        Wallet xd = new Wallet();

        System.out.println("test");

    }
}
