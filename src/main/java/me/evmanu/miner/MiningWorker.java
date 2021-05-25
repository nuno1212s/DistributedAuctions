package me.evmanu.miner;

import lombok.Getter;
import me.evmanu.daos.Hashable;
import me.evmanu.daos.blocks.BlockChain;
import me.evmanu.daos.blocks.BlockChainStandards;
import me.evmanu.daos.blocks.blockbuilders.PoWBlockBuilder;
import me.evmanu.util.ByteHelper;


@Getter
public class MiningWorker implements Runnable {

    private MiningManager miningManager;

    private BlockChain blockBuilder;

    private PoWBlockBuilder individualInstance;

    private long base;

    private long max;

    public MiningWorker(MiningManager miningManager, BlockChain blockBuilder, long base, long max) {
        this.miningManager = miningManager;
        this.blockBuilder = blockBuilder;
        this.base = base;
        this.max = max;

        try {
            this.individualInstance = (PoWBlockBuilder) blockBuilder.getCurrentBlock().clone();
            this.individualInstance.setWorkProof(base);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        forx:
        for (long i = base; i < max; i++) {
            individualInstance.setWorkProof(i);

            byte[] hash = Hashable.calculateHashOf(individualInstance);

            if (ByteHelper.hasFirstBitsSetToZero(hash, BlockChainStandards.ZEROS_REQUIRED)) {
                miningManager.minedBlock(individualInstance);
                break;
            }
        }
    }
}
