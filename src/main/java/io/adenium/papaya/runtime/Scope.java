package io.adenium.papaya.runtime;

import io.adenium.papaya.compiler.Struct;
import io.adenium.core.Block;
import io.adenium.core.BlockIndex;
import io.adenium.core.transactions.Transaction;
import io.adenium.exceptions.ContractOutOfFundsExceptions;
import io.adenium.exceptions.InvalidTransactionException;
import io.adenium.exceptions.PapayaException;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Scope {
    // the 'container' block
    private BlockIndex                          block;
    // the 'caller' transaction
    private Transaction                         caller;
    // the contract in which the point of entry exists
    private Contract                            contract;
    // the stack
    private Stack<PapayaStack<PapayaHandler>>   stack;
    // tell all subprocesses to continue running
    private AtomicBoolean                       keepRunning;
    // an interrupt signal (if any)
    private AtomicInteger                       interruptSignal;
    // the program counter
    private ProgramCounter                      programCounter;
    private PapayaHandler                       nullreference;

    public Scope(Block block, Transaction caller, Contract contract, ProgramCounter programCounter) {
        this.caller     = caller;
        this.contract   = contract;
        this.stack      = new Stack<>();
        this.programCounter = programCounter;
        this.nullreference  = new DefaultHandler(new PapayaObject());
    }

    public long startProcess(long availableFee) throws InvalidTransactionException, PapayaException, ContractOutOfFundsExceptions {
        while (getProgramCounter().hasNext() && keepRunning.get()) {
            OpcodeDefinition opcode = getProgramCounter().next();

            if (availableFee >= opcode.getWeight()) {
                opcode.execute(this);
                availableFee -= opcode.getWeight();
                continue;
            }

            throw new ContractOutOfFundsExceptions();
        }

        return availableFee;
    }

    public void stopProcesses(int signal) {
        keepRunning.set(false);
        interruptSignal.set(signal);
    }

    public Contract getContract() {
        return contract;
    }

    public PapayaStack<PapayaHandler> getStack() {
        return stack.peek();
    }

    public ProgramCounter getProgramCounter() {
        return programCounter;
    }

//    public void checkSig() throws MochaException {
//        MochaObject address     = getStack().pop();
//        MochaObject signature   = getStack().pop();
//        byte signatureData[]    = getSignatureData();
//
//        Key key                 = ((MochaCryptoSignature) signature).getSignature().checkSignature(signatureData);
//
//        if (signature instanceof MochaCryptoSignature) {
//            getStack().push(new MochaBool(false));
//            return;
//        }
//
//        getStack().push(((MochaPublicKey) publicKey).checkSignature((MochaCryptoSignature) signature, signatureData));
//    }

    public void verify() throws InvalidTransactionException, PapayaException {
        if (!getStack().pop().asBool()) {
            throw new InvalidTransactionException();
        }
    }

    protected byte[] getSignatureData() {
        return null;
    }

    public void destroyContract() throws PapayaException {
        // this is the address that we will send any remaining funds to
//        PapayaObject address = getStack().pop();
//
//        if (address instanceof MochaAddress) {
//        }

        throw new PapayaException("invalid address provided.");
    }

    public Stack<Struct> getStackTrace() {
        return null;
    }

    public void callOperator(int operator) throws PapayaException {
        getStack().peek().getStructure().getOperator(operator).call(this);
    }

    public PapayaHandler getNullReference() {
        return nullreference;
    }

    public void makeTuple(int size) throws PapayaException {
        List<PapayaHandler> tuple = new ArrayList<>(size);
        for (int i = size - 1; i > -1; i --) {
            tuple.set(i, stack.peek().pop());
        }

        stack.peek().push(new DefaultHandler(new PapayaTuple(tuple)));
    }

    public void rebase() {
        stack.push(new PapayaStack<>());
    }

    public void drop() {
        stack.pop();
    }

    public void dropKeep(int amount) {
        stack.pop();
    }
}
