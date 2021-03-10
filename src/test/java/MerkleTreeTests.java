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

    MerkleTreeNode mt1 = new MerkleTreeNode(hash1);
    MerkleTreeNode mt2 = new MerkleTreeNode(hash2);
    MerkleTreeNode mt3 = new MerkleTreeNode(hash3);
    MerkleTreeNode mt4 = new MerkleTreeNode(hash4);
    MerkleTreeNode mt5 = new MerkleTreeNode(hash5);
    MerkleTreeNode mt6 = new MerkleTreeNode(hash6);
    MerkleTreeNode mt7 = new MerkleTreeNode(hash7);
    MerkleTreeNode mt8 = new MerkleTreeNode(hash8);
    MerkleTreeNode mt9 = new MerkleTreeNode(hash9);
    MerkleTreeNode mt10 = new MerkleTreeNode(hash10);
    MerkleTreeNode mt11 = new MerkleTreeNode(hash11);

    @Test
    public void testGetList2() {

        LinkedHashMap<byte[], Transaction> transactions = new LinkedHashMap<>();

        transactions.put(hash1, null); // TODO: BUG WITH 2 TRANSACTIONS
        transactions.put(hash2, null);
        transactions.put(hash3, null);
        transactions.put(hash4, null);/*
        transactions.put(hash5, null);
        transactions.put(hash6, null);
        transactions.put(hash7, null);
        transactions.put(hash8, null);
        transactions.put(hash9, null);
        transactions.put(hash10, null);
        transactions.put(hash11, null);
        transactions.put(hash12, null);
        transactions.put(hash13, null);*/

        // GET MERKLE NODES 2.0 --------------------------
        //MerkleTree mtr = new MerkleTree();

        //List<byte[]> listNodes1 = mtr.getMerkleHashes(transactions, hash9);

        // GET ROOT HASH --------------------------
        MerkleTree mt2 = new MerkleTree();

        byte[] rootHash = mt2.getRootHash(transactions);

        // VERIFY TRANSACTION --------------------------
        MerkleTree mt3 = new MerkleTree();

        boolean b = mt3.verifyTransaction(transactions, hash3, rootHash);

        // ------------------------------------------

        System.out.println("Boolean: " + b);

    }




}
