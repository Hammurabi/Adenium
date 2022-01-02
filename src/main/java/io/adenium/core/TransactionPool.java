package io.adenium.core;

import io.adenium.core.transactions.Transaction;
import io.adenium.utils.ByteArray;
import io.adenium.utils.HashQueue;
import io.adenium.utils.Logger;
import io.adenium.utils.PriorityHashQueue;
import org.wolkenproject.utils.*;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class TransactionPool {
    /* keeps track of valid transactions with a "future" nonce */
    private HashQueue<PendingTransaction> futureTransactions;
    private HashQueue<UnconfirmedTransaction>   unconfirmedTransactions;
    private HashQueue<PendingTransaction>       pendingTransactions;
    private ReentrantLock                       mutex;
    private static final int                    MaximumPendingQueueSize     =   125_000_000;
    private static final int                    MaximumUnconfirmedQueueSize =    75_000_000;
    private static final int                    MaximumFuturePoolQueueSize  =    37_500_000;

    public TransactionPool() {
        futureTransactions      = new PriorityHashQueue<>(PendingTransaction::calculateSize);
        pendingTransactions     = new PriorityHashQueue<>(PendingTransaction::calculateSize);
        unconfirmedTransactions = new PriorityHashQueue<>(UnconfirmedTransaction::calculateSize);
        mutex                   = new ReentrantLock();
    }

    public boolean contains(byte[] txid) {
        mutex.lock();
        try {
            return pendingTransactions.containsKey(ByteArray.wrap(txid)) || unconfirmedTransactions.containsKey(ByteArray.wrap(txid));
        } finally {
            mutex.unlock();
        }
    }

    public Set<byte[]> getNonDuplicateTransactions(Set<byte[]> list) {
        Set<byte[]> result = new HashSet<>();

        for (byte[] txid : list) {
            if (!contains(txid)) {
                result.add(txid);
            }
        }

        return result;
    }

    public PendingTransaction getTransaction(byte[] txid) {
        mutex.lock();
        try {
            return pendingTransactions.getByHash(ByteArray.wrap(txid));
        } finally {
            mutex.unlock();
        }
    }

    public Set<byte[]> getHeadOfQueue() {
        Set<byte[]> txids = new LinkedHashSet<>();
        Set<PendingTransaction> transactions = new LinkedHashSet<>();
        int count = 1024;

        for (int i = 0; i < count; i ++) {
            PendingTransaction transaction = pollTransaction();
            if (transaction == null) {
                return txids;
            }

            txids.add(transaction.getHash());
            transactions.add(transaction);
        }

        addPending(transactions);
        return txids;
    }

    public void add(Transaction transaction) {
        add(new PendingTransaction(transaction, System.currentTimeMillis()));
    }

    public void add(PendingTransaction transaction) {
        mutex.lock();
        try {
            addInternally(transaction);
        } finally {
            mutex.unlock();
        }
    }

    protected void addInternally(PendingTransaction transaction) {
        pendingTransactions.add(transaction, transaction.getHash());
        Logger.notify("added transaction '${transaction}'", Logger.Levels.Journaling, transaction);
    }

    private void rejectInternally(PendingTransaction pendingTransaction) {
        UnconfirmedTransaction unconfirmedTransaction = new UnconfirmedTransaction(pendingTransaction, System.currentTimeMillis());
        unconfirmedTransactions.add(unconfirmedTransaction, pendingTransaction.getHash());
        Logger.notify("added transaction '${transaction}'", Logger.Levels.Journaling, unconfirmedTransaction);
        while (unconfirmedTransactions.byteCount() > MaximumUnconfirmedQueueSize) {
            // get the earliest transaction from the top of the queue.
            UnconfirmedTransaction unconfirmed = unconfirmedTransactions.poll();
            // if the transaction is valid again then put it back into the queue.
            if (!unconfirmed.isInvalid()) {
                addInternally(unconfirmedTransaction.getTransaction());
                continue;
            }

            Logger.notify("timed out transaction '${transaction}'", Logger.Levels.Journaling, unconfirmed);
        }
    }

    public void addPending(Collection<PendingTransaction> transactions) {
        for (PendingTransaction transaction : transactions) {
            add(transaction);
        }
    }

    public void add(Collection<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            add(transaction);
        }
    }

    public PendingTransaction pollTransaction() {
        mutex.lock();
        try {
            return pollInternally();
        } finally {
            mutex.unlock();
        }
    }

    private PendingTransaction pollFuture() {
        while (futureTransactions.hasElements()) {
            PendingTransaction pendingTransaction = futureTransactions.poll();

            if (pendingTransaction.isInvalid()) {
                continue;
            }

            return pendingTransaction;
        }

        return null;
    }

    private PendingTransaction pollUnconfirmed() {
        while (unconfirmedTransactions.hasElements()) {
            // poll a transaction from the priority queue.
            UnconfirmedTransaction rejectedTransaction = unconfirmedTransactions.poll();

            // if the transaction is both invalid and has been timed out then we remove it.
            if (rejectedTransaction.isInvalid() && rejectedTransaction.shouldDelete()) {
                continue;
            }

            // if the transaction is not invalid then we return it.
            if (!rejectedTransaction.isInvalid()) {
                return rejectedTransaction.getTransaction();
            }

            // if this is reached then we return null.
            return null;
        }

        return null;
    }

    private PendingTransaction pollPending() {
        while (pendingTransactions.hasElements()) {
            // poll a transaction from the top.
            PendingTransaction pendingTransaction = pendingTransactions.poll();

            // if the transaction is now invalid due to it being included in a block, then reject it.
            if (pendingTransaction.isInvalid()) {
                rejectInternally(pendingTransaction);
                continue;
            }

            return pendingTransaction;
        }

        return null;
    }

    private PendingTransaction pollInternally() {
        PendingTransaction transaction = pollFuture();

        if (transaction == null) {
            transaction = pollUnconfirmed();
        }

        if (transaction == null) {
            return pollPending();
        }

        return transaction;
    }

    public void fillBlock(Block block) {
        while (block.calculateSize() < Context.getInstance().getContextParams().getMaxBlockSize()) {
            PendingTransaction transaction = pollTransaction();

            // if 'poll' is returning null then there are no transactions that we can return.
            if (transaction == null) {
                return;
            }

            // add the transaction to the block.
            block.addTransaction(transaction.getTransaction());

            // check that the block size is still under the limit.
            if (block.calculateSize() > Context.getInstance().getContextParams().getMaxBlockSize()) {
                block.removeLastTransaction();
                add(transaction);
                break;
            }
        }
    }

    private boolean hasPending() {
        mutex.lock();
        try {
            return pendingTransactions.hasElements() || unconfirmedTransactions.hasElements();
        } finally {
            mutex.unlock();
        }
    }
}