package me.evmanu.p2p.kademlia;

import me.evmanu.util.Pair;

import java.math.BigDecimal;
import java.math.BigInteger;

public class P2PTrust {

    private static final float B = 0.65f, W_rr = 0.2f, W_re = 1 - W_rr;


    /**
     * Calculate the risk of the network
     *
     * This risk takes into account the number of low grade interactions expected by the network
     * And the total number of requests made by the peers in the network
     *
     * Since we don't have this information, we just use a relatively high risk factor of 0.3 (1 in every 3 transactions
     * Is a low grade interaction).
     * @return
     */
    private static float calculateR_R() {
        return 0.33f;
    }

    public static BigInteger calculateNewDistance(BigInteger oldDistance, P2PNode node, byte[] destNode) {
//        return (oldDistance * B + (1 - B) * (1 / calculateT(node, destNode)));

        double newDist = oldDistance.doubleValue() * B + (1 - B) * (1 / calculateT(node, destNode));

        return BigDecimal.valueOf(newDist).toBigIntegerExact();
    }

    /**
     * Calculate the risk of the interaction with the given node
     *
     * The risk is given by: + 1/ Np for every correct interaction, - 2/ Np for every wrong interaction
     *
     * @param node The node we are calculating
     * @param destNode The destination node
     * @return
     */
    private static float calculateR_e(P2PNode node, byte[] destNode) {
        Pair<Integer, Integer> registeredInteractions = node.getRegisteredInteractions(destNode);

        int total = registeredInteractions.getKey() + registeredInteractions.getValue();

        if (total == 0) {
            return 1;
        }

        return registeredInteractions.getKey() / ((float) total) - ((registeredInteractions.getValue() * 2) / ((float) total));
    }

    public static float calculateT(P2PNode node, byte[] destinationNode) {
        return W_re * calculateR_e(node, destinationNode) + W_rr * calculateR_R();
    }

}
