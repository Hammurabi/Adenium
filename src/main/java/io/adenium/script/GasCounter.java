package io.adenium.script;

import io.adenium.exceptions.ContractOutOfFundsExceptions;

public class GasCounter {
    private final long amountStart;
    private long amountUsed;

    public GasCounter(long amount) {
        this.amountStart = amount;
        this.amountUsed  = 0;
    }

    public boolean hasRemaining(long amount) {
        return getRemaining() >= amount;
    }

    public long getRemaining() {
        return amountStart - amountUsed;
    }

    public void useGas(long amount) throws ContractOutOfFundsExceptions {
        amountUsed += amount;
        if (getRemaining() < 0) {
            throw new ContractOutOfFundsExceptions();
        }
    }
}
