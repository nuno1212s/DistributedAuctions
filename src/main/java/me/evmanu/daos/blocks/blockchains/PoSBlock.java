package me.evmanu.daos.blocks.blockchains;

import lombok.Getter;
import me.evmanu.Standards;
import me.evmanu.daos.blocks.Block;
import me.evmanu.daos.blocks.BlockChain;
import me.evmanu.daos.blocks.BlockHeader;
import me.evmanu.daos.transactions.ScriptSignature;
import me.evmanu.daos.transactions.Transaction;
import me.evmanu.util.ByteWrapper;

import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public class PoSBlock extends Block {

    /**
     * The ID of the stake that signed this block and the signature of the block
     * <p>
     * This signingID connects to a transaction, made at least 3 blocks behind this one that represents the stake
     * of the user. The signature here has to be valid accordingly to the public key that executed the transaction
     * signingID.
     */
    @Getter
    private final byte[] signingID, signature;

    public PoSBlock(BlockHeader header, LinkedHashMap<ByteWrapper, Transaction> transactions,
                    byte[] signingID, byte[] signature) {
        super(header, transactions);

        this.signingID = signingID;
        this.signature = signature;
    }

    @Override
    public boolean isValid(BlockChain chain) {

        PoSBlockChain blockChain = (PoSBlockChain) chain;

        Optional<Transaction> activeStake = blockChain.getActiveStake(this.signingID, this.getHeader().getBlockNumber());

        if (activeStake.isEmpty()) return false;

        Transaction transaction = activeStake.get();

        ScriptSignature input = transaction.getInputs()[0];

        List<Transaction> allowedSignersForBlock =
                blockChain.getAllowedSignersForBlock(this.getHeader().getBlockNumber());

        if (!allowedSignersForBlock.contains(transaction)) {
            System.out.println("Signer is not one of the allowed signers for this block.");
            return false;
        }

        byte[] publicKey = input.getPublicKey();

        KeyFactory keyFactory = Standards.getKeyFactoryInstance();
        Signature signatureInstance = Standards.getSignatureInstance();

        try {
            signatureInstance.initVerify(keyFactory.generatePublic(new X509EncodedKeySpec(publicKey)));

            this.addToSignature(signatureInstance);

            if (signatureInstance.verify(this.signature)) {
                return true;
            }

        } catch (InvalidKeyException | InvalidKeySpecException | SignatureException e) {
            e.printStackTrace();
        }

        return false;
    }


    @Override
    protected void sub_addToHash(MessageDigest hash) {

        hash.update(this.signingID);
        hash.update(this.signature);

    }
}
