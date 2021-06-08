package me.evmanu.blockchain.blocks.blockchains;

import lombok.Getter;
import me.evmanu.Standards;
import me.evmanu.blockchain.Hashable;
import me.evmanu.blockchain.Signable;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.blocks.BlockChain;
import me.evmanu.blockchain.blocks.BlockHeader;
import me.evmanu.blockchain.blocks.BlockType;
import me.evmanu.blockchain.transactions.ScriptSignature;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.util.ByteWrapper;
import me.evmanu.util.Hex;

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

        if (activeStake.isEmpty()) {

            System.out.println("Active stake is empty");

            return false;
        }

        Transaction transaction = activeStake.get();

        ScriptSignature input = transaction.getInputs()[0];

        List<Transaction> allowedSignersForBlock =
                blockChain.getAllowedSignersForBlock(this.getHeader().getBlockNumber());

        if (!allowedSignersForBlock.contains(transaction)) {
            System.out.println("Signer is not one of the allowed signers for this block.");
            return false;
        }

        byte[] publicKey = input.getPublicKey();

        System.out.println("Public key: " + Hex.toHexString(publicKey));

        KeyFactory keyFactory = Standards.getKeyFactoryInstance();

        try {
            var encodedPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKey));

            if (Signable.verifySignatureOf(this, encodedPublicKey, this.signature)) {
                return true;
            }

            System.out.println("Signature not verified");

        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public void addToSignature(Signature signature) throws SignatureException {
        super.addToSignature(signature);

        signature.update(this.signingID);

    }

    @Override
    protected void sub_addToHash(MessageDigest hash) {

        hash.update(this.signingID);
        hash.update(this.signature);

        System.out.println("Signing ID " + Hex.toHexString(signingID));
        System.out.println("Signature " + Hex.toHexString(signature));

    }

    @Override
    public BlockType getBlockType() {
        return BlockType.PROOF_OF_STAKE;
    }
}
