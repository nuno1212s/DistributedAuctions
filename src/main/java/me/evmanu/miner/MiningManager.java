package me.evmanu.miner;

import me.evmanu.daos.blocks.Block;
import me.evmanu.daos.blocks.BlockChain;
import me.evmanu.daos.blocks.blockbuilders.BlockBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MiningManager {

    private final int threadCount = 16;

    private final ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);

    public List<MiningWorker> workers;

    public BlockChain currentBlockChain;

    public MiningManager(BlockChain currentBlockChain) {
        this.workers = new LinkedList<>();
        this.currentBlockChain = currentBlockChain;

        for (int i = 0; i < threadCount; i++) {
            long aux_base = (Long.MAX_VALUE / threadCount) * i;
            long aux_max = (Long.MAX_VALUE / threadCount) * (i + 1);
            final Future<?> tasks = threadPool.submit(new MiningWorker(this, this.currentBlockChain, aux_base, aux_max));
        }
    }

    public void minedBlock(BlockBuilder block) {

        Block finishedBlock = block.build();
        if (currentBlockChain.verifyBlock(finishedBlock)) {
            currentBlockChain.addBlock(finishedBlock);
        } else
            System.out.println("Bloco inválido");
    }
}
