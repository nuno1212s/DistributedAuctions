package me.evmanu.p2p.kademlia;

import com.google.common.math.BigIntegerMath;
import lombok.Getter;
import me.evmanu.util.Hex;

import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class P2PStandards {

    /**
     * I -> The amount of bits in the key
     * K -> The amount of nodes to store in each K bucket
     * ALPHA -> The parallelization in the network calls
     */
    public static final int I = 160, K = 20, ALPHA = 3;

    /**
     * T_EXPIRE -> The time after which a key/value pair expires; this is a time-to-live (TTL) from the original
     * publication date
     * T_REFRESH -> Time to refresh a K Bucket
     * T_REPLICATE -> Interval between replication events, where a node is required to publish it's entire database
     * T_REPUBLISH -> The time after which the original publisher must republish a key value pair
     */
    public static final long T_EXPIRE = TimeUnit.DAYS.toMillis(1),
            T_REFRESH = TimeUnit.HOURS.toMillis(1),
            T_REPLICATE = TimeUnit.HOURS.toMillis(1),
            T_REPUBLISH = TimeUnit.DAYS.toMillis(1);

    @Getter
    public static final List<NodeTriple> BOOSTRAP_NODES;

    static {
        List<NodeTriple> BOOSTRAP_NODES1;

        try {
            BOOSTRAP_NODES1 = Arrays.asList(new NodeTriple(
                    InetAddress.getByName("51.103.29.195"), 80, Hex.fromHexString("0123456789abcdef0123"), 0
            ));
        } catch (UnknownHostException e) {
            e.printStackTrace();
            BOOSTRAP_NODES1 = Collections.emptyList();
        }

        BOOSTRAP_NODES = BOOSTRAP_NODES1;
    }

    public static int getKBucketFor(byte[] node1, byte[] node2) {
        final var nodeDistance = nodeDistance(node1, node2);

        if (nodeDistance.equals(BigInteger.ZERO)) return 0;

        return BigIntegerMath.log2(nodeDistance, RoundingMode.DOWN);
    }

    public static int getKBucketFor(BigInteger distance) {
        return BigIntegerMath.log2(distance, RoundingMode.DOWN);
    }

    public static BigInteger nodeDistance(BigInteger node1, BigInteger node2) {
        return node1.xor(node2);
    }

    public static BigInteger nodeDistance(byte[] node1, byte[] node2) {
        final var node1ID = new BigInteger(1, node1);
        final var node2ID = new BigInteger(1, node2);

        return nodeDistance(node1ID, node2ID);
    }

}
