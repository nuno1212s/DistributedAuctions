package me.evmanu;

import java.security.*;

public class Standards {

    public static final String DIGEST = "SHA3-256";

    public static final String SIGNING = "SHA3-256withECDSA";

    public static final String KEY_FACTORY = "EC";

    public static MessageDigest getDigestInstance() {
        try {
            return MessageDigest.getInstance(DIGEST);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Signature getSignatureInstance() {
        try {
            return Signature.getInstance(SIGNING);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static KeyFactory getKeyFactoryInstance() {
        try {
            return KeyFactory.getInstance(KEY_FACTORY);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static byte[] calculateHashedPublicKeyFrom(PublicKey key) {

        var digestInstance = getDigestInstance();

        assert digestInstance != null;

        digestInstance.update(key.getEncoded());

        byte[] result1 = digestInstance.digest();

        digestInstance.reset();

        digestInstance.update(result1);

        return digestInstance.digest();

    }

}
