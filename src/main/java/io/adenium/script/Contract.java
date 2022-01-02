package io.adenium.script;

import io.adenium.core.events.DepositFundsEvent;
import io.adenium.core.events.DestroyContractEvent;
import io.adenium.core.events.NewAccountEvent;
import io.adenium.core.transactions.Transaction;
import io.adenium.exceptions.ContractException;
import io.adenium.exceptions.AdeniumException;
import io.adenium.serialization.SerializableI;
import io.adenium.core.Account;
import io.adenium.core.Address;
import io.adenium.core.BlockStateChange;
import io.adenium.core.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class Contract extends SerializableI {
    public static final int IdentifierLength = 20;
    // this is generated when the contract is deployed ripemd(sha3(creator+payload)).
    private final byte  contractId[];

    public Contract(byte contractId[]) {
        this.contractId = contractId;
    }

    protected abstract <T> void onDeploy(Transaction transaction, BlockStateChange blockStateChange, T data);
    protected abstract <T> void onUpdate(Transaction transaction, BlockStateChange blockStateChange, T data);
    protected abstract void onDestroy(Transaction transaction, BlockStateChange blockStateChange, byte[] recipient);

    // this can only be called once.
    public final void deployContract(final Transaction transaction, final BlockStateChange blockStateChange, final Payload payload) throws ContractException {
        onDeploy(transaction, blockStateChange, payload);
    }

    // this will update the internal state of the contract.
    public final void updateContract(final Transaction transaction, final BlockStateChange blockStateChange, final Payload payload) throws ContractException {
        onUpdate(transaction, blockStateChange, payload);
    }

    // the contract is destroyed and all remaining funds are sent to 'recipient'.
    public final void destroyContract(final Transaction transaction, final BlockStateChange blockStateChange, final byte recipient[]) throws ContractException {
        if (recipient.length != Address.RawLength) {
            throw new ContractException("recipient must be a valid 20 byte address.");
        }

        onDestroy(transaction, blockStateChange, recipient);

        if (blockStateChange.checkAccountExists(contractId)) {
            long accountBalance = blockStateChange.getAccountBalance(contractId, true, true);

            if (accountBalance > 0) {
                if (!blockStateChange.checkAccountExists(recipient)) {
                    blockStateChange.addEvent(new NewAccountEvent(recipient));
                }

                blockStateChange.addEvent(new DepositFundsEvent(recipient, accountBalance));
            }
        }

        blockStateChange.addEvent(new DestroyContractEvent(contractId));
    }

    // returns the amount of funds the contract's 'account' has.
    public long getFunds() {
        Account account = Context.getInstance().getDatabase().findAccount(contractId);
        if (account == null) {
            return 0L;
        }

        return account.getBalance();
    }

    public byte[] getContractId() {
        return contractId;
    }

    @Override
    public final void write(OutputStream stream) throws IOException, AdeniumException {
        stream.write(contractId);
        writeContract(stream);
    }

    @Override
    public final void read(InputStream stream) throws IOException, AdeniumException {
        checkFullyRead(stream.read(contractId), IdentifierLength);
        readContract(stream);
    }

    protected abstract void writeContract(OutputStream stream);
    protected abstract void readContract(InputStream stream);
}
