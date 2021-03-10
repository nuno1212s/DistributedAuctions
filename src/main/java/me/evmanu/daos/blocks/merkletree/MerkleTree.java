package me.evmanu.daos.blocks.merkletree;

import lombok.Getter;
import me.evmanu.Standards;
import me.evmanu.daos.transactions.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.lang.Math;

@Getter
public class MerkleTree {

    private MerkleTreeNode root;

    private List<MerkleTreeNode> nodes;

    private List<MerkleTreeNode> leaves;

    public MerkleTree() {
        this.nodes = new ArrayList<>();
        this.leaves = new ArrayList<>();
    }

    enum Direction {
        LEFT,
        RIGHT
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
            //MerkleTreeNode auxNode = new MerkleTreeNode(currentTransaction.getTxID()); // TODO: comentado para testes
            MerkleTreeNode auxNode = new MerkleTreeNode(key);
            addLeaf(auxNode);
        }

        if(oddOrEven(this.leaves.size())) this.leaves.add(null); // se houverem transações impares, coloco uma transação null para ficar par
    }

    public void createParentsLeaves() {

        if(oddOrEven(this.leaves.size())) this.leaves.add(null); // ISTO É PARA TESTE, APAGAR DEPOIS

        // TODO: QUAL A MELHOR MANEIRA DE AVISAR OS ERROS?
        if (this.leaves.size() == 0) System.out.println("Zero transactions in the block");

        for (int i = 0; i < this.leaves.size(); i += 2) {

            MerkleTreeNode parent = new MerkleTreeNode( this.leaves.get(i), this.leaves.get(i + 1) );

            addNode(parent);
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

    public void addNode(MerkleTreeNode node) {
        this.nodes.add(node);
    }


    public byte[] getRootHash(LinkedHashMap<byte[], Transaction> transactions) { // LinkedHashMap<byte[], Transaction> transactions

        this.getTransactionsHashes(transactions);

        this.createParentsLeaves();

        this.makeTree();

        return this.root.getHash();
    }

    public void initMerkleTree(LinkedHashMap<byte[], Transaction> transactions) {

        this.getTransactionsHashes(transactions);

        this.createParentsLeaves();

        this.makeTree();
    }

    public int getIndex(List<MerkleTreeNode> leaves, byte[] targetHash) {

        for (int i = 0; i < leaves.size(); i++) {
            if(leaves.get(i).getHash().equals(targetHash))
                return i;
        }

        return -1;
    }

    public List<byte[]> climbTreeToGetDependentNodes(MerkleTreeNode curNode, List<byte[]> nodeList) {

        Direction curDirection;

        MerkleTreeNode father = curNode.getParentNode();

        if(father.getLeftNode().equals(curNode))
            curDirection = Direction.LEFT;
        else
            curDirection = Direction.RIGHT;


        if(curDirection == Direction.LEFT) {
            if(father.getRightNode() != null)
                nodeList.add(father.getRightNode().getHash());
            else
                nodeList.add(father.getHash());

        } else {
            nodeList.add(father.getLeftNode().getHash());
        }

        if(father.getParentNode() == null) return nodeList;

        nodeList = climbTreeToGetDependentNodes(father, nodeList);

        return nodeList;
    }

    public List<byte[]> getMerkleHashes(LinkedHashMap<byte[], Transaction> transactions, byte[] targetHash) {
        List<byte[]> nodeList = new ArrayList<>();

        initMerkleTree(transactions);

        int transactionIndex = getIndex(this.leaves, targetHash); // TODO: Depois substituir o tipo byte[] por Transaction
        if(transactionIndex == -1) {
            System.out.println("Error, the list does not contain the target transaction");
            return null;
        }

        nodeList = climbTreeToGetDependentNodes(this.leaves.get(transactionIndex), nodeList);

        return nodeList;
    }

    // TODO: change to MerkleVerifiableTransaction?
    public boolean verifyTransaction(LinkedHashMap<byte[], Transaction> transactions, byte[] targetHash, byte[] merkleRoot) {

        List<byte[]> nodeList = getMerkleHashes(transactions, targetHash);

        MerkleTreeNode aux = new MerkleTreeNode(targetHash); // VER ISTO DEPOIS PQ CAUSA DAS CONCAT

        int transactionIndex = getIndex(this.leaves, targetHash); // TODO: USING FOR THE SECOND TIME, DO BETTER!

        byte[] newHash = targetHash;

        for (int i = 0; i < nodeList.size(); i++) {

            if(oddOrEven(transactionIndex)) { //if index its odd, so the node is on the right side
                newHash = aux.concatenateTwoBytes(nodeList.get(i), newHash);
            } else {
                newHash = aux.concatenateTwoBytes(newHash, nodeList.get(i));
            }

            if(transactionIndex > 1)  transactionIndex /= 2; //to know the index of the next hash, in the upper level, so we know if it's on the left/right side
            else transactionIndex = 0; //always left
        }

        if(Arrays.equals(newHash, merkleRoot))
            return true;

        return false;
    }

}

