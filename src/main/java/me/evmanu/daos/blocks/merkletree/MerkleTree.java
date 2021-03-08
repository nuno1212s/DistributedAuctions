package me.evmanu.daos.blocks.merkletree;

import jdk.swing.interop.SwingInterOpUtils;
import lombok.Getter;
import me.evmanu.daos.transactions.Transaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Getter
public class MerkleTree {

    private MerkleTreeNode root;

    private List<MerkleTreeNode> nodes;

    private List<MerkleTreeNode> leaves;

    public MerkleTree() {
        this.nodes = new ArrayList<>();
        this.leaves = new ArrayList<>();
    }

    /**
     * False means even
     * True means odd
     *
     */
    public boolean oddOrEven(int target) {

        if(target % 2 == 0)
            return false;
        else
            return true;
    }

    // 1
    public void getTransactionsHashes(LinkedHashMap<byte[], Transaction> transactions) {

        for(byte[] key : transactions.keySet() ) {
            Transaction currentTransaction = transactions.get(key);
            MerkleTreeNode auxNode = new MerkleTreeNode(currentTransaction.getTxID());
            this.leaves.add(auxNode);
        }

        if(oddOrEven(this.leaves.size())) this.leaves.add(null); // se houverem transações impares, coloco uma transação null para ficar par

    }

    public void createParentsLeaves() {

        if(oddOrEven(this.leaves.size())) this.leaves.add(null); // ISTO É PARA TESTE, APAGAR DEPOIS

        // TODO: QUAL A MELHOR MANEIRA DE AVISAR OS ERROS?
        if (this.leaves.size() == 0) System.out.println("Zero transactions in the block");

        for (int i = 0; i < this.leaves.size(); i += 2) {

            MerkleTreeNode parent = new MerkleTreeNode( this.leaves.get(i), this.leaves.get(i + 1) );

            this.nodes.add(parent);
        }

        if(oddOrEven(this.nodes.size())) this.nodes.add(null);
    }

    //2
    public void makeTree() {

        List<MerkleTreeNode> auxNodes = new ArrayList<>();

        for (int i = 0; i < this.nodes.size(); i += 2) {
            MerkleTreeNode parent = new MerkleTreeNode( this.nodes.get(i), this.nodes.get(i + 1) );

            auxNodes.add(parent);
        }

        this.nodes = auxNodes;

        if(this.nodes.size() == 1) {
            this.root = this.nodes.get(0);

            return;
        }

        if(oddOrEven(this.nodes.size())) this.nodes.add(null);
        
        this.makeTree();
    }


    public byte[] initMerkleTree() { // LinkedHashMap<byte[], Transaction> transactions

        //this.getTransactionsHashes(transactions);

        this.createParentsLeaves();

        this.makeTree();

        return this.root.getHash();
    }

    public static void main(String[] Args) {
        MerkleTree mt = new MerkleTree();

        final byte[] hash1 = {(byte) 0x01, (byte) 0x12, (byte) 0x03, (byte) 0x04};
        final byte[] hash2 = {(byte) 0xae, (byte) 0x44, (byte) 0x98, (byte) 0xff};
        final byte[] hash3 = {(byte) 0x5f, (byte) 0xd3, (byte) 0xcc, (byte) 0xe1};
        final byte[] hash4 = {(byte) 0xcb, (byte) 0xbc, (byte) 0xc4, (byte) 0xe2};
        final byte[] hash5 = {(byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04};

        MerkleTreeNode mt1 = new MerkleTreeNode(hash1);
        MerkleTreeNode mt2 = new MerkleTreeNode(hash2);
        MerkleTreeNode mt3 = new MerkleTreeNode(hash3);
        MerkleTreeNode mt4 = new MerkleTreeNode(hash4);
        MerkleTreeNode mt5 = new MerkleTreeNode(hash5);
        MerkleTreeNode mt6 = new MerkleTreeNode(hash5);

        mt.leaves.add(mt1);
        mt.leaves.add(mt2);
        mt.leaves.add(mt3);
        mt.leaves.add(mt4);
        mt.leaves.add(mt5);
        //mt.leaves.add(mt6);

        byte[] finalHash = mt.initMerkleTree();

        System.out.println("HASH: " + finalHash);

    }


}
