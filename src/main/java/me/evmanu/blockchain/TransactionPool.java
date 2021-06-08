package me.evmanu.blockchain;

import com.google.common.collect.ImmutableList;
import me.evmanu.blockchain.transactions.Transaction;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class TransactionPool {

    private final AtomicInteger transactionCount = new AtomicInteger(0);

    private final LinkedList<Transaction> pendingTransactionsBacking = new LinkedList<>();

    private final List<Transaction> pendingTransactions = Collections.synchronizedList(pendingTransactionsBacking);

    private final List<BiConsumer<Integer, Transaction>> subscribers = Collections.synchronizedList(new LinkedList<>());

    private final BlockChainHandler handler;

    public TransactionPool(BlockChainHandler handler) {
        this.handler = handler;
    }

    public int currentPoolSize() {
        return this.pendingTransactions.size();
    }

    public void registerSubscriberForTransactionReception(BiConsumer<Integer, Transaction> consumer) {
        subscribers.add(consumer);
    }

    public boolean receiveTransaction(Transaction transaction) {

        if (transaction == null) {
            throw new NullPointerException("Cannot receive a null transaction");
        }

        if (this.handler.getBestCurrentChain().isPresent()) {
            if (!this.handler.getBestCurrentChain().get().verifyTransaction(transaction)) {
                return false;
            }
        }

        int currentCount = transactionCount.incrementAndGet();

        this.pendingTransactions.add(transaction);

        synchronized (subscribers) {
            for (BiConsumer<Integer, Transaction> subscriber : subscribers) {
                subscriber.accept(currentCount, transaction);
            }
        }

        return true;
    }

    public ImmutableList<Transaction> getFirstNTransactions(int N) {

        int toRemove = 0, newSize, prevSize;

        do {
            prevSize = this.transactionCount.get();
            newSize = prevSize;

            if (newSize <= N) {
                toRemove = newSize;

                newSize = 0;
            } else {
                toRemove = N;

                newSize -= N;
            }


        } while (!this.transactionCount.compareAndSet(prevSize, newSize));

        this.transactionCount.accumulateAndGet(-toRemove, Integer::sum);

        ImmutableList.Builder<Transaction> builder = ImmutableList.builder();

        synchronized (this.pendingTransactions) {

            for (int i = 0; i < toRemove && !pendingTransactions.isEmpty(); i++) {
                Transaction transaction = this.pendingTransactionsBacking.pollFirst();

                if (transaction != null)
                    builder.add(transaction);
            }

        }

        return builder.build();
    }

}
