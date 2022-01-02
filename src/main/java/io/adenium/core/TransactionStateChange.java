package io.adenium.core;

import io.adenium.core.transactions.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionStateChange {
    private final BlockStateChange  stateChange;
    private List<Event>             transactionEvents;

    public TransactionStateChange(BlockStateChange stateChange) {
        this.stateChange = stateChange;
        this.transactionEvents = new ArrayList<>();
    }

    public final boolean resolve(Transaction transaction) {
//        if (transaction.verify(this)) {
//            stateChange.addEvents(transactionEvents);
//            return true;
//        }

        return false;
    }
}
