package me.evmanu;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class Standards {

    public static final String DIGEST = "SHA3-256";

    public static final String SIGNING = "SHA256withECDSA";

    public static final String KEY_FACTORY = "EC";

    public static final String KEY_AGREEMENT = "ECDH";

    public static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";

    public static final int TAG_LENGTH_BIT = 128;
    public static final int IV_LENGTH_BYTE = 12;
    public static final int AES_KEY_BIT = 256;

    public static final SecureRandom secureRandom = new SecureRandom();

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

    public static KeyAgreement getKeyAgreement() {

        try {
            return KeyAgreement.getInstance(KEY_AGREEMENT);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static byte[] generateSharedSecret(byte[] publicKey, PrivateKey privateKey) {
        var keyAgreement = Standards.getKeyAgreement();

        var x509EncodedKeySpec = new X509EncodedKeySpec(publicKey);

        try {
            var auctioneerPK = Standards.getKeyFactoryInstance().generatePublic(x509EncodedKeySpec);

            keyAgreement.init(privateKey);

            keyAgreement.doPhase(auctioneerPK, true);

            return keyAgreement.generateSecret();
        } catch (InvalidKeyException | InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return new byte[0];
    }

    public static byte[] encryptTextWithPrependedIV(byte[] textToCypher, byte[] secretKey) {

        byte[] iv = new byte[IV_LENGTH_BYTE];

        secureRandom.nextBytes(iv);

        var parsedSecret = new SecretKeySpec(secretKey, 0, secretKey.length, "AES");

        try {
            var cipher = Cipher.getInstance(CIPHER_ALGORITHM);

            cipher.init(Cipher.ENCRYPT_MODE, parsedSecret, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            return cipher.doFinal(textToCypher);

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException
                | NoSuchPaddingException | BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return new byte[0];
    }

    public static byte[] decryptWithPrefixIV(byte[] cText, byte[] secret) {

        ByteBuffer bb = ByteBuffer.wrap(cText);

        var parsedSecret = new SecretKeySpec(secret, 0, secret.length, "AES");

        byte[] iv = new byte[IV_LENGTH_BYTE];
        bb.get(iv);
        //bb.get(iv, 0, iv.length);

        byte[] cipherText = new byte[bb.remaining()];
        bb.get(cipherText);

        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, parsedSecret, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            return cipher.doFinal(cText);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException
                | IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {

            e.printStackTrace();
        }

        return new byte[0];
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

    public static byte[] concatenateTwoBytes(byte[] a, byte[] b) {

        byte[] c = new byte[a.length + b.length];

        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);

        return c;
    }

}
