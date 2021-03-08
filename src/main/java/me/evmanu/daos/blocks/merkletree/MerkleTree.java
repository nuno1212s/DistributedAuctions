package me.evmanu.daos.blocks.merkletree;

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

    public void addLeaf(MerkleTreeNode node) {
        this.leaves.add(node);
    }

    public byte[] initMerkleTree() { // LinkedHashMap<byte[], Transaction> transactions

        //this.getTransactionsHashes(transactions);

        this.createParentsLeaves();

        this.makeTree();

        return this.root.getHash();
    }

}
