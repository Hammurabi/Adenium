package io.adenium.papaya;

import io.adenium.core.transactions.Transaction;
import io.adenium.exceptions.ContractOutOfFundsExceptions;
import io.adenium.exceptions.InvalidTransactionException;
import io.adenium.exceptions.PapayaException;
import io.adenium.core.Address;

public abstract class Script {
    public static byte[] newP2PKH(Address address) {
        return new byte[0];
    }

    public abstract void fromCompressedFormat(byte data[]);
    public abstract byte[] getCompressed();

    public static long executePayload(Transaction transaction) throws ContractOutOfFundsExceptions, InvalidTransactionException, PapayaException {
//        // create a program counter from opcodes
//        ProgramCounter programCounter = new ProgramCounter(ByteBuffer.wrap(transaction.getPayload()), Context.getInstance().getOpcodeRegister());
//
//        // create the contract object
//        MochaObject contract = Contract.newContract(transaction.getRecipient());
//
//        // create the transaction object
//        MochaObject transactionObject = new MochaObject(false);
//        transactionObject.addMember(new MochaBool(transaction.hasMultipleSenders()));
//        transactionObject.addMember(new MochaBool(transaction.hasMultipleRecipients()));
//        transactionObject.addMember(new MochaNumber(transaction.getVersion(), false));
//        transactionObject.addMember(new MochaNumber(transaction.getTransactionValue(), false));
//        transactionObject.addMember(new MochaNumber(transaction.getTransactionFee(), false));
//
//        // create the stack and populate it
//        MochaStack<MochaObject> stack = new MochaStack<>();
//        stack.push(contract);
//        stack.push(transactionObject);
//
//        // maximum amount to spend
//        long maxSpend = transaction.getTransactionFee();
        return 0;
    }
}
