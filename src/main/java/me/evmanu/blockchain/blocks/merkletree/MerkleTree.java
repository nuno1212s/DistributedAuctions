package me.evmanu.blockchain.blocks.merkletree;

import lombok.Getter;
import me.evmanu.Standards;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.util.ByteWrapper;
import me.evmanu.util.Hex;

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

    public void addLeaf(MerkleTreeNode node) {
        this.leaves.add(node);
    }

    public void addNode(MerkleTreeNode node) { this.nodes.add(node); }

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

    /**
     *  Take every single hash of each transaction and
     *  put them on a merkleTreeNode list of leaves
     */
    public void getTransactionsHashes(LinkedHashMap<ByteWrapper, Transaction> transactions) {

        for(ByteWrapper key : transactions.keySet() ) {
            Transaction currentTransaction = transactions.get(key);
            //MerkleTreeNode auxNode = new MerkleTreeNode(currentTransaction.getTxID()); // TODO: comentado para testes, pois estou a fornecer diretamente o valor de hash
            MerkleTreeNode auxNode = new MerkleTreeNode(key.getBytes()); // TODO: apagar
            addLeaf(auxNode);
        }

        if(oddOrEven(this.leaves.size())) this.leaves.add(null); // se houverem transações impares, coloco uma transação null para ficar par
    }

    /**
     * Create the first list of parent nodes, starting from the
     * bottom, which in this case represents the list of leaves
     */
    public void createParentsLeaves() {

        //if(oddOrEven(this.leaves.size())) this.leaves.add(null); // ISTO É PARA TESTE, APAGAR DEPOIS

        if (this.leaves.size() == 0) System.out.println("Zero transactions in the block");

        for (int i = 0; i < this.leaves.size(); i += 2) {

            MerkleTreeNode parent = new MerkleTreeNode( this.leaves.get(i), this.leaves.get(i + 1) );

            addNode(parent);
        }

        if(oddOrEven(this.nodes.size())) this.nodes.add(null);
    }

    /**
     * - Recursive method, which takes the list of the previous parent nodes
     * and calculate their parents
     * - The recursion ends when the parent list size is equal to one,
     * aka root of the tree
     */
    public void makeTree() {

        List<MerkleTreeNode> auxNodes = new ArrayList<>();

        if(this.leaves.size() > 2) {

            for (int i = 0; i < this.nodes.size(); i += 2) {
                MerkleTreeNode parent = new MerkleTreeNode(this.nodes.get(i), this.nodes.get(i + 1));

                auxNodes.add(parent);
            }

        } else { // if the tree has 1 or 2 transactions
            this.root = this.nodes.get(0);

            return;
        }

        this.nodes = auxNodes;

        if(this.nodes.size() == 1) {
            this.root = this.nodes.get(0);

            return;
        }

        if(oddOrEven(this.nodes.size())) this.nodes.add(null);
        
        this.makeTree();
    }

    public byte[] getRootHash(LinkedHashMap<ByteWrapper, Transaction> transactions) { // LinkedHashMap<byte[], Transaction> transactions

        this.getTransactionsHashes(transactions);

        this.createParentsLeaves();

        this.makeTree();

        return this.root.getHash();
    }

    public void initMerkleTree(LinkedHashMap<ByteWrapper, Transaction> transactions) {

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

    public List<byte[]> climbTreeToGetDependentNodes(MerkleTreeNode curNode, List<byte[]> nodeList, byte[] targetHash) {

        Direction curDirection;

        MerkleTreeNode father = curNode.getParentNode();

        if(father.getLeftNode().equals(curNode))
            curDirection = Direction.LEFT;
        else
            curDirection = Direction.RIGHT;


        if(curDirection == Direction.LEFT) {

            if (father.getRightNode() != null) {
                nodeList.add(father.getRightNode().getHash());
            } else if (father.getHash() != targetHash) {
                if (father.getLeftNode().getLeftNode() == null)
                    nodeList.add(father.getHash());
                //else
                    //nodeList.add( nodeList.get(nodeList.size()-1) ); // repeated
            }

        } else {
            nodeList.add(father.getLeftNode().getHash());
        }

        if(father.getParentNode() == null) return nodeList;

        nodeList = climbTreeToGetDependentNodes(father, nodeList, targetHash);

        return nodeList;
    }

    public List<byte[]> getMerkleHashes(LinkedHashMap<ByteWrapper, Transaction> transactions, byte[] targetHash) {
        List<byte[]> nodeList = new ArrayList<>();

        if(this.root == null) initMerkleTree(transactions); // avoiding to double initialize trees (it can generate conflict), because if root its not null, the tree has already been initialized in another method

        int transactionIndex = getIndex(this.leaves, targetHash); // TODO: Depois substituir o tipo byte[] por Transaction
        if(transactionIndex == -1) {
            System.out.println("Error, the list does not contain the target transaction");
            return null;
        }

        nodeList = climbTreeToGetDependentNodes(this.leaves.get(transactionIndex), nodeList, targetHash);

        return nodeList;
    }

    // TODO: change to MerkleVerifiableTransaction?
    public boolean verifyTransaction(LinkedHashMap<ByteWrapper, Transaction> transactions, byte[] targetHash, byte[] merkleRoot) {

        List<byte[]> nodeList = getMerkleHashes(transactions, targetHash);

        MerkleTreeNode aux = new MerkleTreeNode(targetHash); // VER ISTO DEPOIS PQ CAUSA DAS CONCAT

        int transactionIndex = getIndex(this.leaves, targetHash); // TODO: USING FOR THE SECOND TIME, DO BETTER!

        byte[] newHash = targetHash;

        if(nodeList.size() > 2 || this.root.getLeftNode().getLeftNode() == null) {

            for (int i = 0; i < nodeList.size(); i++) {

                byte[] currentHash = nodeList.get(i);

                    if (oddOrEven(transactionIndex)) { //if index its odd, so the node is on the right side
                        newHash = aux.generateHash(currentHash, newHash);
                    } else {
                        newHash = aux.generateHash(newHash, currentHash);
                    }

                if (transactionIndex > 1)
                    transactionIndex /= 2; //to know the index of the next hash, in the upper level, so we know if it's on the left/right side
                else transactionIndex = 0; //always left

            }

        } else if(nodeList.size() == 2) { // condition if on the right branch just exist always a odd number of leaves
            if (oddOrEven(transactionIndex)) { //if index its odd, so the node is on the right side
                newHash = aux.generateHash(nodeList.get(0), newHash);
            } else {
                newHash = aux.generateHash(newHash, nodeList.get(0));
            }

            newHash = aux.generateHash(nodeList.get(1), newHash);

        } else { // condition if on the right branch just exist one transaction
            newHash = aux.generateHash(nodeList.get(0), newHash);
        }

        String s1 = Hex.toHexString(newHash);
        String s2 = Hex.toHexString(merkleRoot);

        System.out.println("ORIGINAL MERKLE ROOT: " + s2);
        System.out.println("NEW HASH COMPUTED WITH THE TARGET TRANSACTION: " + s1);

        if(Arrays.equals(newHash, merkleRoot))
            return true;

        return false;
    }

}

