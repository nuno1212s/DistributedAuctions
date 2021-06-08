package me.evmanu.p2p.kademlia;


import me.evmanu.Standards;
import me.evmanu.util.ByteHelper;
import me.evmanu.util.Hex;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Random;

/**
 * Computation resource challenge, to verify if the a node that is joining the network has computational power
 * And is willing to give it to the network (Prevents Sybil attacks)
 */
public class CRChallenge {

    private static final Random random = new Random();

    /**
     * The amount of zeroes that the hash of the challenge + response must have
     */
    public static final int CRC_DIFFICULTY = 1;


    public static long generateRandomChallenge() {
        return random.nextLong();
    }

    public static boolean verifyCRChallenge(long challenge, long response) {

        MessageDigest digest = Standards.getDigestInstance();

        ByteBuffer bytes = ByteBuffer.allocate(Long.BYTES * 2);

        bytes.putLong(challenge);
        bytes.putLong(response);

        digest.update(bytes.array());

        byte[] hashResult = digest.digest();

        return ByteHelper.hasFirstBitsSetToZero(hashResult, CRC_DIFFICULTY);
    }

    public static long solveCRChallenge(long challenge) {

        MessageDigest digest = Standards.getDigestInstance();

        for (long response = 0; ;response++) {
            ByteBuffer bytes = ByteBuffer.allocate(Long.BYTES * 2);

            bytes.putLong(challenge);
            bytes.putLong(response);

            digest.update(bytes.array());

            byte[] hashResult = digest.digest();

            if (ByteHelper.hasFirstBitsSetToZero(hashResult, CRC_DIFFICULTY)) {
                return response;
            }

            digest.reset();
        }
    }
}
