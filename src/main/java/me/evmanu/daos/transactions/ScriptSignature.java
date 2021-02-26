package me.evmanu.daos.transactions;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.evmanu.Standards;
import me.evmanu.daos.Hashable;

import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScriptSignature implements Hashable {

    private final byte[] originatingTXID;

    private final int outputIndex;

    private final byte[] publicKey, signature;

    @Override
    public void addToHash(MessageDigest digest) {
        var buffer = ByteBuffer.allocate(originatingTXID.length + 4 + publicKey.length + signature.length);

        buffer.put(originatingTXID);
        buffer.putInt(outputIndex);
        buffer.put(publicKey);
        buffer.put(signature);

        digest.update(buffer.array());
    }

    /**
     * Verifies that the output that originates this input exists in the transaction that it's supposed to, at the
     * index position that it's supposed to
     *
     * This does not verify the validity of the originatingTransaction
     *
     * @param originatingTransaction
     * @return
     */
    public boolean verifyOutputExists(Transaction originatingTransaction) {

        if (!Arrays.equals(originatingTXID, originatingTransaction.getTxID())) {
            System.out.println("The transaction provided is not the transaction that originated this input!");
            return false;
        }

        if (this.outputIndex >= originatingTransaction.getOutputs().length) {
            //This should never happen as we've verified that the transaction is indeed the transaction that originated it,
            //but never hurts to check, as the originatingTransaction might not be valid
            System.out.println("Size error.");
            return false;
        }

        final KeyFactory keyFactoryInstance = Standards.getKeyFactoryInstance();

        assert keyFactoryInstance != null;

        final var output = originatingTransaction.getOutputs()[this.outputIndex];

        var pubKey = new X509EncodedKeySpec(this.publicKey);

        try {
            var hashedResult = Standards.calculateHashedPublicKeyFrom(keyFactoryInstance.generatePublic(pubKey));

            return Arrays.equals(output.getHashedPubKey(), hashedResult);

        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Verifies that the signature was made by the public key that is stored in this object
     *
     * @return
     */
    public boolean verifyOwnership() {

        final Signature signatures = Standards.getSignatureInstance();

        final KeyFactory keyFactoryInstance = Standards.getKeyFactoryInstance();

        assert signatures != null && keyFactoryInstance != null;

        X509EncodedKeySpec pubKey = new X509EncodedKeySpec(this.publicKey);

        try {
            final var publicKey = keyFactoryInstance.generatePublic(pubKey);

            signatures.initVerify(publicKey);

            signatures.update(this.getOriginatingTXID());
            signatures.update(ByteBuffer.allocate(4).putInt(outputIndex));
            signatures.update(this.publicKey);

            return signatures.verify(this.signature);

        } catch (InvalidKeySpecException | SignatureException | InvalidKeyException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Create an input from an output of a previous transaction
     * This does not verify the transaction that is provided, but it does verify that the key pair that was provided
     * Matches the key pair that corresponds to the public key which the output belongs to.
     *
     * @param transaction
     * @param outputIndex
     * @param correspondingKeyPair
     * @return
     * @throws IllegalAccessException
     */
    public static ScriptSignature fromOutput(Transaction transaction, int outputIndex, KeyPair correspondingKeyPair)
            throws IllegalAccessException {

        ScriptPubKey output = transaction.getOutputs()[outputIndex];

        var result = Standards.calculateHashedPublicKeyFrom(correspondingKeyPair.getPublic());

        if (!Arrays.equals(result, output.getHashedPubKey())) {
            throw new IllegalAccessException("That key pair does not match the output you are requesting!");
        }

        final Signature signatures = Standards.getSignatureInstance();

        byte[] resultSignature;

        try {
            signatures.initSign(correspondingKeyPair.getPrivate());

            signatures.update(transaction.getTxID());

            signatures.update(ByteBuffer.allocate(4).putInt(outputIndex));

            signatures.update(correspondingKeyPair.getPublic().getEncoded());

            resultSignature = signatures.sign();

        } catch (InvalidKeyException | SignatureException e) {
            e.printStackTrace();

            return null;
        }

        return new ScriptSignature(transaction.getTxID(), outputIndex,
                correspondingKeyPair.getPublic().getEncoded(),
                resultSignature);
    }
}
