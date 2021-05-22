import me.evmanu.daos.blocks.merkletree.MerkleTree;
import me.evmanu.daos.blocks.merkletree.MerkleTreeNode;
import me.evmanu.daos.transactions.Transaction;
import me.evmanu.util.ByteWrapper;
import me.evmanu.util.Hex;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

public class MerkleTreeTests {

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

    // ---------------------------------------- VALUES TO CHANGE, FOR TESTS ----------------------------------------
    int nTransactions = 9; // max its 13 -> nTransactions defines the number of transactions that we want to generate

    byte[] transactionTarget = hash1; // needs to be less or equal of nTransactions

    byte[] badHash = hash4; // by default, the correct hash its hash2 -> badHash serves to change the list of hashes, so it is possible to test a tampered transactions
    // -------------------------------------------------------------------------------------------------------------

    MerkleTree mt = new MerkleTree();

    byte[][] validTransactions = {hash1, hash2, hash3, hash4, hash5, hash6, hash7, hash8, hash9, hash10, hash11, hash12, hash13};

    // Same list as the previous one, but with the second hash different, to simulate a bad transaction
    byte[][] badTransactions = {hash1, badHash, hash3, hash4, hash5, hash6, hash7, hash8, hash9, hash10, hash11, hash12, hash13};

    public LinkedHashMap<ByteWrapper, Transaction> getTestTransactionsX(int x, byte[][] hashList) {

        LinkedHashMap<ByteWrapper, Transaction> transactions = new LinkedHashMap<>();

        assert hashList.length >= x : "Chosen number greater than the number of hashes available";

        for (int i = 0; i < x; i++)
            transactions.put(new ByteWrapper(hashList[i]), null);

        return transactions;
    }

    LinkedHashMap<ByteWrapper, Transaction> validTransactionsList = getTestTransactionsX(nTransactions, validTransactions);

    LinkedHashMap<ByteWrapper, Transaction> badTransactionsList = getTestTransactionsX(nTransactions, badTransactions);

    @Test
    public void printTestHashes() {

        System.out.print("Hashes that are going to be leaves: [ ");
        for(ByteWrapper key : validTransactionsList.keySet() ) {
            Transaction currentTransaction = validTransactionsList.get(key);

            String s = Hex.toHexString(key);

            System.out.print(s + " ");
        }
        System.out.println("]");
    }

    @Test
    public void testGettingRootHash() {

        byte[] rootHash = mt.getRootHash(validTransactionsList);

        String s = Hex.toHexString(rootHash);

        System.out.println("Root Hash: " + s);

        System.out.println("-----------------------------------------");

        assert rootHash.length != 0 : "Root Hash its empty";
    }

    @Test
    public void testGettingDependentTransactionNodeList() {

        List<byte[]> nodeList = mt.getMerkleHashes(validTransactionsList, transactionTarget); // the second input needs to be a valid transaction

        String s0 = Hex.toHexString(transactionTarget);

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

        byte[] rootHash = mt2.getRootHash(validTransactionsList); // credible root hash

        LinkedHashMap<ByteWrapper, Transaction> transactionsToTest = badTransactionsList; // transaction on 2º position modified

        // 2º position its a modified transaction, so the result of that root hash will be different from the previous one
        boolean b = mt.verifyTransaction(transactionsToTest, hash1, rootHash); // the second input means the transaction target

        assert !b : "The target transaction must not be a valid one!";

        System.out.println("Boolean result of modified transaction: " + b);
    }

    @Test
    public void verifyTransactionVeracityWithGoodTransaction() {

        System.out.println("-----------------------------------------");

        MerkleTree aux = new MerkleTree();
        byte[] rootHash = aux.getRootHash(validTransactionsList);

        boolean b = mt.verifyTransaction(validTransactionsList, transactionTarget, rootHash); // the second input means the transaction target

        System.out.println("Boolean result of good transaction: " + b);

        assert b : "The target transaction must be a valid one!";

        System.out.println("-----------------------------------------");
    }

}