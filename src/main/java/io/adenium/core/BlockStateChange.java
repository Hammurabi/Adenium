package io.adenium.core;

import io.adenium.core.events.DepositFundsEvent;
import io.adenium.core.events.NewAccountEvent;
import io.adenium.core.events.RegisterAliasEvent;
import io.adenium.core.events.WithdrawFundsEvent;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class BlockStateChange {
    private Queue<byte[]>   transactionIds;
    private Queue<byte[]>   transactionEventIds;
    private List<Event>     transactionEvents;
    private int             blockHeight;

    public BlockStateChange(int blockHeight) {
        this.transactionIds         = new LinkedList<>();
        this.transactionEventIds    = new LinkedList<>();
        this.transactionEvents      = new LinkedList<>();
        this.blockHeight            = blockHeight;
    }

    public TransactionStateChange push() {
        return new TransactionStateChange(this);
    }

    public boolean checkAliasExists(long alias) {
        if (Context.getInstance().getDatabase().checkAccountExists(alias)) {
            return true;
        }

        for (Event event : transactionEvents) {
            if (event instanceof RegisterAliasEvent) {
                if (((RegisterAliasEvent) event).getAlias() == alias) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean checkAccountExists(byte address[]) {
        if (Context.getInstance().getDatabase().checkAccountExists(address)) {
            return true;
        }

        for (Event event : transactionEvents) {
            if (event instanceof NewAccountEvent) {
                if (Arrays.equals(((NewAccountEvent) event).getAddress(), address)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void addEvent(Event event) {
        transactionEvents.add(event);
        transactionEventIds.add(event.eventId());
    }

    public void addEvents(List<Event> events) {
        transactionEvents.addAll(events);
        for (Event event : events) {
            transactionEventIds.add(event.eventId());
        }
    }

    public BlockStateChangeResult getResult() {
        return new BlockStateChangeResult(transactionIds, transactionEventIds, transactionEvents);
    }

    public void addTransaction(byte[] transactionID) {
        transactionIds.add(transactionID);
    }

    public void createAccountIfDoesNotExist(byte address[]) {
        if (checkAccountExists(address)) {
            return;
        }

        addEvent(new NewAccountEvent(address));
    }

    // returns the balance for the account associated with 'address'
    // if 'afterDeposit' is true then DepositEvents from this state change are summed.
    // if 'afterWithdraw' is true then WithdrawEvents from this state change are summed.
    public long getAccountBalance(byte[] address, boolean afterDeposit, boolean afterWithdraw) {
        long balance = 0L;

        if (Context.getInstance().getDatabase().checkAccountExists(address)) {
            balance = Context.getInstance().getDatabase().findAccount(address).getBalance();
        }

        for (Event event : transactionEvents) {
            if (event instanceof DepositFundsEvent && afterWithdraw) {
                balance += ((DepositFundsEvent) event).getAmount();
            } else if (event instanceof WithdrawFundsEvent && afterWithdraw) {
                balance -= ((WithdrawFundsEvent) event).getAmount();
            }
        }

        return balance;
    }

    public int getBlockHeight() {
        return blockHeight;
    }
}
