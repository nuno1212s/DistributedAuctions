import me.evmanu.daos.blocks.merkletree.MerkleTree;
import me.evmanu.daos.blocks.merkletree.MerkleTreeNode;
import org.junit.jupiter.api.Test;

public class MerkleTreeTests {

    @Test
    public void testMerkleTree() {
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

            mt.addLeaf(mt1);
            mt.addLeaf(mt2);
            mt.addLeaf(mt3);
            mt.addLeaf(mt4);
            mt.addLeaf(mt5);
            //mt.leaves.add(mt6);

            byte[] finalHash = mt.initMerkleTree();

            System.out.println("HASH: " + finalHash);
    }

}
