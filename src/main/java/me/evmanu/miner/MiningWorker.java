package me.evmanu.miner;

import lombok.Getter;
import me.evmanu.daos.blocks.BlockChain;
import me.evmanu.daos.blocks.blockbuilders.BlockBuilder;
import me.evmanu.daos.blocks.blockbuilders.PoWBlockBuilder;

@Getter
public class MiningWorker implements Runnable {

    private MiningManager miningManager;

    private BlockChain blockBuilder;

    private PoWBlockBuilder individualInstance;

    public MiningWorker(MiningManager miningManager, BlockChain blockBuilder) {
        this.miningManager = miningManager;
        this.blockBuilder = blockBuilder;

        try {
            this.individualInstance = (PoWBlockBuilder) blockBuilder.getCurrentBlock().clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        final PoWBlockBuilder currentBlock = (PoWBlockBuilder) blockBuilder.getCurrentBlock();

        //work work work

    }

}
