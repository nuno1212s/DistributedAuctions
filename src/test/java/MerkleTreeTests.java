import me.evmanu.daos.blocks.merkletree.MerkleTree;
import me.evmanu.daos.blocks.merkletree.MerkleTreeNode;
import me.evmanu.daos.transactions.Transaction;
import me.evmanu.util.Hex;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

public class MerkleTreeTests {
    /*
    final byte[] hash1 = {(byte) 0x01, (byte) 0x12, (byte) 0x03, (byte) 0x04};
    final byte[] hash2 = {(byte) 0xae, (byte) 0x44, (byte) 0x98, (byte) 0xff};
    final byte[] hash3 = {(byte) 0x5f, (byte) 0xd3, (byte) 0xcc, (byte) 0xe1};
    final byte[] hash4 = {(byte) 0xcb, (byte) 0xbc, (byte) 0xc4, (byte) 0xe2};
    final byte[] hash5 = {(byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04};
    final byte[] hash6 = {(byte) 0x20, (byte) 0x55, (byte) 0x03, (byte) 0x11};
    final byte[] hash7 = {(byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd};
    final byte[] hash8 = {(byte) 0xee, (byte) 0x02, (byte) 0xff, (byte) 0x04};
    final byte[] hash9 = {(byte) 0x22, (byte) 0x46, (byte) 0xbc, (byte) 0xa3};*/

    final byte[] hash1 = {(byte) 0x01};
    final byte[] hash2 = {(byte) 0xae};
    final byte[] hash3 = {(byte) 0x5f};
    final byte[] hash4 = {(byte) 0xcb};
    final byte[] hash5 = {(byte) 0x09};
    final byte[] hash6 = {(byte) 0x20};
    final byte[] hash7 = {(byte) 0xaa};
    final byte[] hash8 = {(byte) 0xee};
    final byte[] hash9 = {(byte) 0x22};
    final byte[] hash10 = {(byte) 0x16};
    final byte[] hash11 = {(byte) 0x77};
    final byte[] hash12 = {(byte) 0xed};
    final byte[] hash13 = {(byte) 0xef};

    MerkleTree mt = new MerkleTree();

    public LinkedHashMap<byte[], Transaction> getTestTransactions() {

        LinkedHashMap<byte[], Transaction> transactions = new LinkedHashMap<>();

        transactions.put(hash1, null);
        transactions.put(hash2, null);
        transactions.put(hash3, null);
        transactions.put(hash4, null);
        transactions.put(hash5, null);
        transactions.put(hash6, null);
        transactions.put(hash7, null);
        transactions.put(hash8, null);
        transactions.put(hash9, null);
        transactions.put(hash10, null);
        transactions.put(hash11, null);
        transactions.put(hash12, null);
        transactions.put(hash13, null); /*  */

        return transactions;
    }

    public LinkedHashMap<byte[], Transaction> getTestTransactions2() {

        LinkedHashMap<byte[], Transaction> transactions = new LinkedHashMap<>();

        transactions.put(hash1, null);
        transactions.put(hash2, null);
        transactions.put(hash3, null);
        transactions.put(hash13, null);
        transactions.put(hash5, null);
        transactions.put(hash6, null);
        transactions.put(hash7, null);
        transactions.put(hash8, null);
        transactions.put(hash9, null);
        transactions.put(hash10, null);/*
        transactions.put(hash11, null);
        transactions.put(hash12, null);
        transactions.put(hash13, null);*/

        return transactions;
    }

    LinkedHashMap<byte[], Transaction> transactions = getTestTransactions();


    @Test
    public void printTestHashes() {

        System.out.print("Hashes that are going to be leaves: [ ");
        for(byte[] key : transactions.keySet() ) {
            Transaction currentTransaction = transactions.get(key);

            String s = Hex.toHexString(key);

            System.out.print(s + " ");
        }
        System.out.println("]");
    }

    @Test
    public void testGettingRootHash() {

        byte[] rootHash = mt.getRootHash(transactions);

        String s = Hex.toHexString(rootHash);

        System.out.println("Root Hash: " + s);

        System.out.println("-----------------------------------------");
    }

    @Test
    public void testGettingDependentTransactionNodeList() {

        List<byte[]> nodeList = mt.getMerkleHashes(transactions, aux); // the second input needs to be a valid transaction

        String s0 = Hex.toHexString(aux);

        System.out.print("Dependent node List of ["+ s0 +"] : [ ");
        for (int i = 0; i < nodeList.size(); i++) {

            String s = Hex.toHexString(nodeList.get(i));

            System.out.print(s + " ");
        }

        System.out.println("]");
    }

    @Test
    public void verifyTransactionVeracityWithBadTransaction() {

        System.out.println("-----------------------------------------");

        MerkleTree mt2 = new MerkleTree();

        byte[] rootHash = mt2.getRootHash(transactions); // credible root hash

        LinkedHashMap<byte[], Transaction> transactionsToTest = getTestTransactions2(); // transaction on 3º position modified

        // hash13 its the modified transaction, so the result of that root hash will be different from the previous one
        boolean b = mt.verifyTransaction(transactionsToTest, hash13, rootHash); // the second input means the transaction target

        System.out.println("Boolean result of modified transaction: " + b);
    }

    @Test
    public void verifyTransactionVeracityWithGoodTransaction() {

        System.out.println("-----------------------------------------");

        byte[] rootHash = mt.getRootHash(transactions);

        boolean b = mt.verifyTransaction(transactions, aux, rootHash); // the second input means the transaction target

        System.out.println("Boolean result of good transaction: " + b);

        System.out.println("-----------------------------------------");
    }

    byte[] aux = hash10;
}
