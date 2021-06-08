package me.evmanu.miner;

import lombok.Setter;
import me.evmanu.blockchain.BlockChainHandler;
import me.evmanu.blockchain.BlockChainStandards;
import me.evmanu.blockchain.TransactionPool;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.blocks.blockbuilders.BlockBuilder;
import me.evmanu.blockchain.blocks.blockbuilders.PoWBlockBuilder;
import me.evmanu.blockchain.blocks.blockchains.PoWBlockChain;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.util.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class MiningManager {

    private final int threadCount = 1;

    private final ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);

    @Setter
    private PoWBlockChain blockChain;

    private BlockChainHandler blockChainHandler;

    public List<Pair<MiningWorker, Future<?>>> workers;

    private final List<Consumer<Block>> minedBlockListeners;

    private final AtomicBoolean currentlyWorking = new AtomicBoolean(false);

    private final TransactionPool transactionPool;

    public MiningManager(TransactionPool transactionPool, PoWBlockChain blockChain) {
        this.workers = new LinkedList<>();
        this.minedBlockListeners = new LinkedList<>();
        this.transactionPool = transactionPool;
        this.blockChain = blockChain;
    }

    public MiningManager(TransactionPool transactionPool, BlockChainHandler handler) {
        this.workers = new LinkedList<>();
        this.minedBlockListeners = new LinkedList<>();
        this.transactionPool = transactionPool;
        this.blockChainHandler = handler;

        if (this.transactionPool != null) {
            transactionPool.registerSubscriberForTransactionReception(this::receiveTransaction);
        }
    }

    public void registerMinedBlockListener(Consumer<Block> blockConsumer) {
        this.minedBlockListeners.add(blockConsumer);
    }

    private void receiveTransaction(int transactionCount, Transaction transaction) {

        if (transactionCount >= BlockChainStandards.MIN_TRANSACTION_COUNT &&
                this.currentlyWorking.compareAndSet(false, true)) {

            System.out.println("Reached minimum block amount, starting to mine a block.");

            var bestCurrentChain = this.blockChainHandler.getBestCurrentChain();

            if (bestCurrentChain.isPresent()) {

                var latestValidBlock = bestCurrentChain.get().getLatestValidBlock();

                PoWBlockBuilder poWBlockBuilder;

                var transactions = this.transactionPool.getFirstNTransactions(Math.min(
                        BlockChainStandards.MAX_TRANSACTION_COUNT,
                        transactionCount
                ));

                if (latestValidBlock != null) {
                    var header = latestValidBlock.getHeader();

                    poWBlockBuilder = PoWBlockBuilder.fromTransactionList(header.getBlockNumber() + 1, bestCurrentChain.get().getVersion(),
                            header.getBlockHash(),
                            transactions);
                } else {
                    poWBlockBuilder = PoWBlockBuilder.fromTransactionList(0,
                            bestCurrentChain.get().getVersion(),
                            new byte[0],
                            transactions);
                }

                assignWork(poWBlockBuilder);
            }
        }
    }

    public void cancelWorkers() {
        for (Pair<MiningWorker, Future<?>> worker : this.workers) {
            worker.getValue().cancel(true);
        }

        workers.clear();
        this.currentlyWorking.set(false);
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

        this.currentlyWorking.set(true);

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

        if (blockChainHandler == null) {
            if (blockChain.verifyBlock(finishedBlock)) {

                blockChain.addBlock(finishedBlock);

                for (Consumer<Block> minedBlockListener : this.minedBlockListeners) {
                    minedBlockListener.accept(finishedBlock);
                }

                cancelWorkers();

                return true;
            } else
                System.out.println("Bloco inválido");
        } else {
            if (blockChainHandler.addBlockToChainAndUpdate(finishedBlock)) {

                for (Consumer<Block> minedBlockListener : this.minedBlockListeners) {
                    minedBlockListener.accept(finishedBlock);
                }

                cancelWorkers();
                return true;
            } else {
                System.out.println("Block inválido para a chain atual");
            }
        }

        return false;
    }
}
