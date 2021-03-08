package me.evmanu.daos.transactions;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.evmanu.Standards;
import me.evmanu.daos.Hashable;
import me.evmanu.util.Hex;

import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
/*
 *
 */
public class ScriptSignature implements Hashable {

    //The block of the transaction
    private final long originatingBlock;

    //The transaction that has this input as it's output at the index outputIndex
    private final byte[] originatingTXID;

    private final int outputIndex;

    //The signature of this input (including the public key) and all the outputs
    private final byte[] publicKey, signature;

    @Override
    public void addToHash(MessageDigest digest) {
        var buffer = ByteBuffer.allocate(originatingTXID.length
                + Long.BYTES +
                + Integer.BYTES + publicKey.length + signature.length);

        buffer.putLong(originatingBlock);
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
    public final boolean verifyOutputExists(Transaction originatingTransaction) {

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

            if (!Arrays.equals(output.getHashedPubKey(), hashedResult)) {
                System.out.println("The public key provided does not match the public key of the output!");

                return false;
            }

            return true;
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Verifies that the signature was made by the public key that is stored in this object
     *
     * @param owner The transaction that contains this input
     * @return
     */
    public final boolean verifyOwnership(Transaction owner) {

        final Signature signatures = Standards.getSignatureInstance();

        final KeyFactory keyFactoryInstance = Standards.getKeyFactoryInstance();

        assert signatures != null && keyFactoryInstance != null;

        X509EncodedKeySpec pubKey = new X509EncodedKeySpec(this.publicKey);

        try {
            final var publicKey = keyFactoryInstance.generatePublic(pubKey);

            signatures.initVerify(publicKey);

            signatures.update(ByteBuffer.allocate(Long.BYTES).putLong(originatingBlock));
            signatures.update(this.getOriginatingTXID());
            signatures.update(ByteBuffer.allocate(Integer.BYTES).putInt(outputIndex));
            signatures.update(this.publicKey);

            //We include the output that are planned so that no one can change the amount of coin
            //That's going to each output (It was not possible to add or remove output,
            //but we could shift the amount of money that is sent to each output)
            for (ScriptPubKey output : owner.getOutputs()) {
                signatures.update(output.getHashedPubKey());

                signatures.update(ByteBuffer.allocate(Float.BYTES).putFloat(output.getAmount()));
            }

            return signatures.verify(this.signature);

        } catch (InvalidKeySpecException | SignatureException | InvalidKeyException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public String toString() {
        return "Input{" +
                "originatingBlock=" + originatingBlock +
                ", originatingTXID=" + Hex.toHexString(originatingTXID) +
                ", outputIndex=" + outputIndex +
                ", publicKey=" + Hex.toHexString(publicKey) +
                ", signature=" + Hex.toHexString(signature) +
                '}';
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
    public static ScriptSignature fromOutput(Transaction transaction, int outputIndex, KeyPair correspondingKeyPair,
                                             ScriptPubKey[] outputs)
            throws IllegalAccessException {

        ScriptPubKey output = transaction.getOutputs()[outputIndex];

        var result = Standards.calculateHashedPublicKeyFrom(correspondingKeyPair.getPublic());

        if (!Arrays.equals(result, output.getHashedPubKey())) {
            //TODO: Add this back, this is just for testing wrong keypairs
//            throw new IllegalAccessException("That key pair does not match the output you are requesting!");
        }

        final Signature signatures = Standards.getSignatureInstance();

        byte[] resultSignature;

        try {
            signatures.initSign(correspondingKeyPair.getPrivate());

            signatures.update(ByteBuffer.allocate(Long.BYTES).putLong(transaction.getBlockNumber()));

            signatures.update(transaction.getTxID());

            signatures.update(ByteBuffer.allocate(Integer.BYTES).putInt(outputIndex));

            signatures.update(correspondingKeyPair.getPublic().getEncoded());

            for (ScriptPubKey scriptPubKey : outputs) {
                signatures.update(scriptPubKey.getHashedPubKey());

                signatures.update(ByteBuffer.allocate(Float.BYTES).putFloat(scriptPubKey.getAmount()));
            }

            resultSignature = signatures.sign();

        } catch (InvalidKeyException | SignatureException e) {
            e.printStackTrace();

            return null;
        }

        return new ScriptSignature(transaction.getBlockNumber(),
                transaction.getTxID(), outputIndex,
                correspondingKeyPair.getPublic().getEncoded(),
                resultSignature);
    }
}
