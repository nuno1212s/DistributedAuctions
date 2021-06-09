package me.evmanu.blockchain.blocks.blockbuilders;

import lombok.Setter;
import me.evmanu.blockchain.Hashable;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.blocks.BlockHeader;
import me.evmanu.blockchain.blocks.blockchains.PoSBlock;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.util.Hex;

import java.security.MessageDigest;
import java.security.Signature;
import java.security.SignatureException;
import java.util.List;

@Setter
public class PoSBlockBuilder extends BlockBuilder {

    private byte[] signingID;
    private byte[] signature;

    public PoSBlockBuilder(long blockNumber, short version, byte[] previousBlockHash) {
        super(blockNumber, version, previousBlockHash);
    }

    @Override
    protected void sub_addToHash(MessageDigest hash) {

        hash.update(this.signingID);
        hash.update(this.signature);

        System.out.println("Signing ID " + Hex.toHexString(signingID));
        System.out.println("Signature " + Hex.toHexString(signature));

    }

    @Override
    protected void sub_addToSignature(Signature signature) throws SignatureException {

        signature.update(this.signingID);

    }

    @Override
    public Block build() {

        var blockHash = Hashable.calculateHashOf(this);
        var header = new BlockHeader(this.getBlockNumber(), this.getVersion(), this.timeGenerated.get(), getPreviousBlockHash(),
                blockHash,
                this.getMerkleRoot());

        System.out.println("Calculated hash: " + Hex.toHexString(blockHash));

        return new PoSBlock(header, getTransactionsCurrentlyInBlock(), signingID, signature);
    }

    public static PoSBlockBuilder fromTransactionList(long blockNum, short version,
                                               byte[] prevHash, List<Transaction> transactions) {

        var poSBlockBuilder = new PoSBlockBuilder(blockNum, version, prevHash);

        poSBlockBuilder.setTransactions(transactions);

        return poSBlockBuilder;
    }

}
