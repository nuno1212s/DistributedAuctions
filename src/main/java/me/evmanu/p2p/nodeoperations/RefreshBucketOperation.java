package me.evmanu.p2p.nodeoperations;

import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.P2PStandards;
import me.evmanu.util.ByteHelper;

import java.util.BitSet;

public class RefreshBucketOperation implements Operation {

    private final int kBucket;

    private final P2PNode node;

    private boolean finished = false;

    public RefreshBucketOperation(int kBucket, P2PNode node) {
        this.kBucket = kBucket;
        this.node = node;
    }

    @Override
    public void execute() {

        //An ID that is 2^kBucket < dist(ID, node.ID) < 2^kBucket+1 for us to perform a lookup operation on

        System.out.println("Refreshing bucket " + this.kBucket);

        this.node.registerOngoingOperation(this);

        byte[] kBucketID = generateIDForBucket(this.node, kBucket);

        NodeLookupOperation lookupOperation = new NodeLookupOperation(node, kBucketID,
                (_nodes) -> setFinished(true));

        lookupOperation.execute();
    }

    public void setFinished(boolean finished) {
        this.finished = finished;

        if (finished) {
            this.node.registerOperationDone(this);
        }
    }

    @Override
    public boolean hasFinished() {
        return finished;
    }

    private static byte[] generateIDForBucket(P2PNode node, int kBucket) {

        byte[] result = new byte[P2PStandards.I / Byte.SIZE];

        /* Since distance = ID_LENGTH - prefixLength, we need to fill that amount with 0's */
        int numByteZeroes = (P2PStandards.I - kBucket) / Byte.SIZE;
        int numBitZeroes = Byte.SIZE - (kBucket % Byte.SIZE);

        /* Filling byte zeroes */
        for (int i = 0; i < numByteZeroes; i++) {
            result[i] = 0x00;
        }

        /* Filling bit zeroes */
        BitSet bits = new BitSet(8);
        bits.set(0, 8);

        for (int i = 0; i < numBitZeroes; i++) {
            /* Shift 1 zero into the start of the value */
            bits.clear(i);
        }

        bits.flip(0, 8);        // Flip the bits since they're in reverse order
        result[numByteZeroes] = bits.toByteArray()[0];

        /* Set the remaining bytes to Maximum value */
        for (int i = numByteZeroes + 1; i < result.length; i++) {
            result[i] = Byte.MAX_VALUE;
        }

        return ByteHelper.xor(node.getNodeID(), result);
    }
}
