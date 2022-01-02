package io.adenium.script;

import io.adenium.core.transactions.Transaction;
import io.adenium.core.Address;
import io.adenium.core.BlockStateChange;

public class Invoker {
    private final BlockStateChange  blockStateChange;
    private final Transaction transaction;
    private final Address           invoker;

    public Invoker(BlockStateChange blockStateChange, Transaction transaction, Address invoker) {
        this.blockStateChange = blockStateChange;
        this.transaction = transaction;
        this.invoker = invoker;
    }

    public BlockStateChange getBlockStateChange() {
        return blockStateChange;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public Address getInvoker() {
        return invoker;
    }
}
