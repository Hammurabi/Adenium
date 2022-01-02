package io.adenium.core;

public class UnconfirmedTransaction implements Comparable<UnconfirmedTransaction> {
    private final PendingTransaction    transaction;
    private final long                  when;

    public UnconfirmedTransaction(PendingTransaction transaction, long when) {
        this.transaction = transaction;
        this.when = when;
    }

    public boolean isInvalid() {
        return transaction.isInvalid();
    }

    public boolean shouldDelete() {
        return System.currentTimeMillis() - when > Context.getInstance().getContextParams().getMaxTransactionUnconfirmedTime();
    }

    @Override
    public int compareTo(UnconfirmedTransaction o) {
        return when < o.when ? -1 : 1;
    }

    public int calculateSize() {
        return transaction.calculateSize();
    }

    public PendingTransaction getTransaction() {
        return transaction;
    }

    @Override
    public String toString() {
        return transaction.toString();
    }
}
