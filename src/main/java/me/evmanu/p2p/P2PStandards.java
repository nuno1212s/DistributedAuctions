package me.evmanu.p2p;

import com.google.common.math.BigIntegerMath;

import java.math.BigInteger;
import java.math.RoundingMode;

public class P2PStandards {

    public static final int I = 160, K = 20;

    public static int getKBucketFor(byte[] node1, byte[] node2) {

        final var nodeDistance = nodeDistance(node1, node2);

        return BigIntegerMath.log2(nodeDistance, RoundingMode.DOWN);
    }

    public static BigInteger nodeDistance(byte[] node1, byte[] node2) {
        final var node1ID = new BigInteger(1, node1);
        final var node2ID = new BigInteger(1, node2);

        return node1ID.xor(node2ID);
    }

}
