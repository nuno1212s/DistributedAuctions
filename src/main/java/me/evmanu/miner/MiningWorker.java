package me.evmanu.miner;

import lombok.Getter;
import me.evmanu.daos.Hashable;
import me.evmanu.daos.blocks.BlockChain;
import me.evmanu.daos.blocks.PoWBlock;
import me.evmanu.daos.blocks.blockbuilders.BlockBuilder;
import me.evmanu.daos.blocks.blockbuilders.PoWBlockBuilder;
import me.evmanu.util.ByteHelper;

import static me.evmanu.daos.blocks.PoWBlock.ZEROS_REQUIRED;

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

            for (int block = 0; block < (int) Math.ceil(ZEROS_REQUIRED / (float) Byte.SIZE); block++) {
                byte byteBlock = hash[block];

                final var byteWithFirstOnes = ByteHelper.getByteWithFirstOnes(ZEROS_REQUIRED);

                //TO check if the first x bits are 0, we do an and with a byte where the first x bits are 1,
                //And the rest are 0, so when we AND them, we must get 0
                //Example: 0001 1111 & 1110 0000 = 0
                if ((byteBlock & byteWithFirstOnes) != 0) {
                    continue forx;
                }
            }

            miningManager.minedBlock(individualInstance);
            break;
        }
    }
}
