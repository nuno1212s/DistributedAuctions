package me.evmanu.blockchain.blocks;

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

    public int currentPoolSize() {
        return this.pendingTransactions.size();
    }

    public void registerSubscriberForTransactionReception(BiConsumer<Integer, Transaction> consumer) {
        subscribers.add(consumer);
    }

    public void receiveTransaction(Transaction transaction) {
        int currentCount = transactionCount.incrementAndGet();

        this.pendingTransactions.add(transaction);

        synchronized (subscribers) {
            for (BiConsumer<Integer, Transaction> subscriber : subscribers) {
                subscriber.accept(currentCount, transaction);
            }
        }
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
