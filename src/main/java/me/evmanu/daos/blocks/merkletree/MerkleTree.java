package me.evmanu.daos.blocks.merkletree;

import lombok.Getter;
import me.evmanu.daos.transactions.Transaction;

import java.util.ArrayList;
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

    public void initMerkleTree2(LinkedHashMap<byte[], Transaction> transactions) {

        this.getTransactionsHashes(transactions);

        this.createParentsLeaves();

        this.makeTree();
    }

    public int getTreeLevel(int leavesNumber)  {

        float logCalc = (float) (Math.log(leavesNumber) / Math.log(2)); // log2(n)

        int aux = (int) logCalc; // verify integer

        if(logCalc % aux == 0 || logCalc == 1)
            return (int) logCalc;

        return 1 + ((int) logCalc);
    }

    public int getIndex(List<MerkleTreeNode> leaves, byte[] targetHash) {

        for (int i = 0; i < leaves.size(); i++) {
            if(leaves.get(i).getHash().equals(targetHash))
                return i;
        }

        return -1;
    }

    public List<byte[]> recursive(MerkleTreeNode curNode, List<byte[]> nodeList, float[] leavesRange, int treeLevel,
                                  int curLevel, int transactionIndex) {

        float halfTree = (int) (  (leavesRange[1] - (leavesRange[0]-1)) / 2);

        if(treeLevel != curLevel) {

            // left side
            if(transactionIndex < ((leavesRange[1] + 1 ) - halfTree)) {

                if(curNode.getRightNode() != null) {
                    nodeList.add(curNode.getRightNode().getHash());

                    curNode = curNode.getLeftNode();
                } else {
                    curNode = curNode.getLeftNode();
                }

                // TODO: Quando um ramo direto contem nulls, ver isso

                leavesRange[1] -= halfTree;

            // right side
            } else {

                nodeList.add(curNode.getLeftNode().getHash());

                curNode = curNode.getRightNode();

                leavesRange[0] += halfTree;
            }

            nodeList = recursive(curNode, nodeList, leavesRange, treeLevel, curLevel + 1, transactionIndex);
        }

        return nodeList;
    }

    //(LinkedHashMap<byte[], Transaction> transactions, Transaction targetTransaction)
    public List<byte[]> getMerkleHashes(LinkedHashMap<byte[], Transaction> transactions, byte[] targetHash) {

        List<byte[]> nodeList = new ArrayList<>();

        initMerkleTree2(transactions);

        int transactionIndex = getIndex(this.leaves, targetHash); // TODO: Depois substituir byte[] por Transaction
        if(transactionIndex == -1) {
            System.out.println("Error, the list does not contain the target transaction");
            return null;
        }

        int treeLevel = getTreeLevel(this.leaves.size());
        float maxLeaves = (float) Math.pow(2,treeLevel); // maximum number of leaves, according to the level of the tree

        float[] leavesRange = new float[2];
        leavesRange[0] = 0;
        leavesRange[1] = maxLeaves - 1;

        nodeList = recursive(this.getRoot(), nodeList, leavesRange, treeLevel, 0, transactionIndex);

        return nodeList;
    }

}
