package me.evmanu.blockchain;

import me.evmanu.Standards;

import java.security.*;

public interface Signable {

    void addToSignature(Signature signature) throws SignatureException;

    static byte[] calculateSignatureOf(Signable hashable, PrivateKey privateKey) {

        Signature signatureInstance = Standards.getSignatureInstance();

        try {
            signatureInstance.initSign(privateKey);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        try {
            hashable.addToSignature(signatureInstance);

            return signatureInstance.sign();
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        return new byte[0];
    }

    static boolean verifySignatureOf(Signable signable, PublicKey key, byte[] signature) {

        Signature signatureInstance = Standards.getSignatureInstance();

        try {
            signatureInstance.initVerify(key);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        try {
            signable.addToSignature(signatureInstance);

            return signatureInstance.verify(signature);
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        return false;
    }

}
