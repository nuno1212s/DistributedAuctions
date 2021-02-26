package me.evmanu.miner;

import me.evmanu.daos.blocks.BlockChain;

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
            final Future<?> tasks = threadPool.submit(new MiningWorker(this.currentBlockChain));
        }
    }



}
