package io.adenium.core;

import io.adenium.core.transactions.Transaction;

public class PendingTransaction implements Comparable<PendingTransaction> {
    private final Transaction transaction;
    private final long          when;

    public PendingTransaction(Transaction transaction, long when) {
        this.transaction = transaction;
        this.when = when;
    }

    public boolean isInvalid() {
        return transaction.checkTransaction() == Transaction.TransactionCode.InvalidTransaction;
    }

    public boolean shouldDelete() {
        return System.currentTimeMillis() - when > Context.getInstance().getContextParams().getMaxTransactionUnconfirmedTime();
    }

    @Override
    public int compareTo(PendingTransaction o) {
        return transaction.getTransactionFee() > o.transaction.getTransactionFee() ? -1 : 1;
    }

    public int calculateSize() {
        return transaction.calculateSize();
    }

    public byte[] getHash() {
        return transaction.getHash();
    }

    public Transaction getTransaction() {
        return transaction;
    }

    @Override
    public String toString() {
        return transaction.toString();
    }
}
