package me.evmanu;

import java.security.*;

public class Standards {

    public static final String DIGEST = "SHA3-256";

    public static final String SIGNING = "SHA256withECDSA";

    public static final String KEY_FACTORY = "EC";

    static {
//        Security.addProvider(new BouncyCastleProvider());
    }

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

    public static KeyPairGenerator getKeyGenerator() {

        try {
            return KeyPairGenerator.getInstance(KEY_FACTORY);
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

    // TODO
    public static byte[] calculateHashedFromByte(byte[] target) {
        var digestInstance = getDigestInstance();

        assert digestInstance != null;

        digestInstance.update(target);

        return digestInstance.digest();
    }

}
