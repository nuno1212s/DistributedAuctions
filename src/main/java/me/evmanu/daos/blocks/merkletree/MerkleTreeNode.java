package me.evmanu.daos.blocks.merkletree;

import lombok.Getter;
import lombok.Setter;
import me.evmanu.Standards;
import me.evmanu.util.Hex;

@Getter
public class MerkleTreeNode {

    @Setter
    private byte[] hash;

    private MerkleTreeNode leftNode;

    private MerkleTreeNode rightNode;

    private MerkleTreeNode parentNode;

    public MerkleTreeNode(byte[] hash) {

        this.hash = hash;
        this.leftNode = null;
        this.rightNode = null;
    }

    public MerkleTreeNode(MerkleTreeNode leftNode, MerkleTreeNode rightNode) {

        this.leftNode = leftNode;
        this.rightNode = rightNode;
        this.leftNode.parentNode = this;

        if(this.rightNode != null) {
            this.rightNode.parentNode = this;
            this.hash = generateHash( this.leftNode.getHash(), this.rightNode.getHash() );
        } else {
            this.hash = this.leftNode.getHash();
        }
    }

    public byte[] generateHash(byte[] leftNode, byte[] rightNode) {

        byte[] mergedHashes = concatenateTwoBytes(leftNode, rightNode);

        //return Standards.calculateHashedFromByte(mergedHashes); // COMENTADO PARA TESTE

        return mergedHashes;
    }

    public byte[] concatenateTwoBytes(byte[] a, byte[] b) {

        byte[] c = new byte[a.length + b.length];

        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);

        return c;
    }

}
