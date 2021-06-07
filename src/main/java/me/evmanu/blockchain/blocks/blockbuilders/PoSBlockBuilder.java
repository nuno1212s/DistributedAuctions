package me.evmanu.blockchain.blocks.blockbuilders;

import lombok.Setter;
import me.evmanu.blockchain.Hashable;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.blocks.BlockHeader;
import me.evmanu.blockchain.blocks.blockchains.PoSBlock;

import java.security.MessageDigest;

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

    }

    @Override
    public Block build() {

        var header = new BlockHeader(this.getBlockNumber(), this.getVersion(), System.currentTimeMillis(), getPreviousBlockHash(),
                Hashable.calculateHashOf(this),
                this.getMerkleRoot());

        return new PoSBlock(header, getTransactionsCurrentlyInBlock(), signingID, signature);
    }
}
