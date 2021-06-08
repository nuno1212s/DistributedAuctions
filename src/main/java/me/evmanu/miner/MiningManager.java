package me.evmanu.miner;

import lombok.Setter;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.blocks.BlockChain;
import me.evmanu.blockchain.blocks.TransactionPool;
import me.evmanu.blockchain.blocks.blockbuilders.BlockBuilder;
import me.evmanu.blockchain.blocks.blockbuilders.PoWBlockBuilder;
import me.evmanu.blockchain.blocks.blockchains.PoWBlockChain;
import me.evmanu.util.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MiningManager {

    private final int threadCount = 1;

    private final ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);

    @Setter
    private PoWBlockChain blockChain;

    public List<Pair<MiningWorker, Future<?>>> workers;

    private TransactionPool transactionPool;

    public MiningManager(TransactionPool transactionPool, PoWBlockChain blockChain) {
        this.workers = new LinkedList<>();
        this.transactionPool = transactionPool;
        this.blockChain = blockChain;
    }

    public void cancelWorkers() {
        for (Pair<MiningWorker, Future<?>> worker : this.workers) {
            worker.getValue().cancel(true);
        }

        workers.clear();
    }

    public void blockOnWorkers() {
        for (Pair<MiningWorker, Future<?>> worker : workers) {
            try {
                worker.getValue().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public void assignWork(PoWBlockBuilder builder) {

        for (Pair<MiningWorker, Future<?>> worker : this.workers) {
            worker.getValue().cancel(true);
        }

        workers.clear();

        for (int i = 0; i < threadCount; i++) {
            long aux_base = (Long.MAX_VALUE / threadCount) * i;
            long aux_max = (Long.MAX_VALUE / threadCount) * (i + 1);
            MiningWorker worker = new MiningWorker(this, builder, aux_base, aux_max);

            /*final Future<?> tasks =
                    threadPool.submit(worker);

            this.workers.add(Pair.of(worker, tasks));*/

            worker.run();
        }
    }


    public boolean minedBlock(BlockBuilder block) {

        Block finishedBlock = block.build();

        if (blockChain.verifyBlock(finishedBlock)) {
            blockChain.addBlock(finishedBlock);

            cancelWorkers();

            return true;
        } else
            System.out.println("Bloco inválido");

        return false;
    }
}
