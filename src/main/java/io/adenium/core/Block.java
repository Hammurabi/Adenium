package io.adenium.core;

import io.adenium.core.transactions.MintTransaction;
import io.adenium.core.transactions.Transaction;
import io.adenium.exceptions.AdeniumException;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.ChainMath;
import io.adenium.utils.Utils;
import io.adenium.utils.VarInt;

import java.io.*;
import java.math.BigInteger;
import java.util.*;

public class Block extends SerializableI implements Iterable<Transaction> {
    public static int               UniqueIdentifierLength  = 32;
    private BlockHeader             blockHeader;
    private Set<Transaction>        transactions;
    private BlockStateChangeResult  stateChange;

    public Block() {
        this(new byte[32], 0);
    }

    public Block(byte previousHash[], int bits)
    {
        blockHeader = new BlockHeader(Context.getInstance().getContextParams().getVersion(), Utils.timestampInSeconds(), previousHash, new byte[32], bits, 0);
        transactions = new LinkedHashSet<>();
    }

    public final int calculateSize() {
        int transactionLength = 0;
        for (Transaction transaction : transactions) {
            transactionLength += transaction.calculateSize();
        }

        return BlockHeader.Size + VarInt.sizeOfCompactUin32(transactions.size(), false) + transactionLength;
    }

    public final int calculateSizeLocalStorage() {
        int transactionLength = 0;
        for (Transaction transaction : transactions) {
            transactionLength += transaction.calculateSize();
        }

        return blockHeader.calculateSize() + VarInt.sizeOfCompactUin32(transactions.size(), false) + transactionLength;
    }

    /*
        returns a new block header
     */
    public final BlockHeader getBlockHeader() {
        return new BlockHeader(getVersion(), getTimestamp(), getParentHash(), getMerkleRoot(), getBits(), getNonce());
    }

    // executes transctions and returns an event list
    public BlockStateChangeResult getStateChange() {
        return stateChange;
    }

    // call transaction.verify()
    // this does not mean that transactions are VALID
    private boolean shallowVerifyTransactions() {
        for (Transaction transaction : transactions) {
            if (transaction.checkTransaction() == Transaction.TransactionCode.InvalidTransaction) {
                return false;
            }
        }

        return true;
    }

    public void build(int blockHeight) throws AdeniumException {
        MintTransaction mint = (MintTransaction) getTransactions().iterator().next();
        mint.setFees(getFees());
        // create the state change object.
        createSateChange(blockHeight);
        // set the combined merkle root.
        setMerkleRoot(getStateChange().getMerkleRoot());
    }

    public boolean verify(BlockHeader lastDifficultyChange, BlockHeader parent, int blockHeight) {
        // check that this is not the genesis block.
        if (blockHeight > 0) {
            // reject the block if the timestamp is more than 144 seconds ahead.
            if (getTimestamp() - System.currentTimeMillis() > Context.getInstance().getContextParams().getMaxFutureBlockTime()) return false;
            // reject the block if the timestamp is older than the parent's timestamp.
            if (getTimestamp() <= parent.getTimestamp()) return false;
            // check bits are set correctly.
            if (blockHeight % Context.getInstance().getContextParams().getDifficultyAdjustmentThreshold() == 0) {
                if (getBits() != ChainMath.generateTargetBits(getBlockHeader(), lastDifficultyChange)) return false;
            } else {
                if (getBits() != parent.getBits()) return false;
            }
        } else {
            // check bits for block 0.
            if (getBits() != Context.getInstance().getContextParams().getDefaultBits()) return false;
        }
        // PoW check.
        if (!blockHeader.verifyProofOfWork()) return false;
        // must have at least one transaction.
        if (transactions.isEmpty()) return false;
        // first transaction must be a minting transaction.
        if (transactions.iterator().next() instanceof MintTransaction == false) return false;
        // check that no other mint transactions exist.
        if (checkOverMinting()) return false;
        // shallow transaction checks.
        if (!shallowVerifyTransactions()) return false;
        // create a state change object and verify transactions.
        if (!createSateChange(blockHeight)) return false;
        // merkle tree checks.
        if (!Utils.equals(getStateChange().getMerkleRoot(), getMerkleRoot())) return false;

        return true;
    }

    private boolean checkOverMinting() {
        int mints = 0;
        for (Transaction transaction : transactions) {
            if (transaction instanceof MintTransaction) {
                mints ++;
            }
        };

        return mints > 1;
    }

    // creates a state change and verifies transactions
    private boolean createSateChange(int blockHeight) {
        BlockStateChange blockStateChange = new BlockStateChange(blockHeight);

        long fees = 0L;

        for (Transaction transaction : transactions) {
            fees += transaction.getTransactionFee();
        }

        for (Transaction transaction : transactions) {
            if (!transaction.verify(blockStateChange, this, blockHeight, fees)) {
                return false;
            }

            //TODO:
            try {
                transaction.getStateChange(this, blockStateChange);
            } catch (AdeniumException e) {
                return false;
            }

            blockStateChange.addTransaction(transaction.getHash());
        }

        stateChange = blockStateChange.getResult();
        return true;
    }

    public byte[] getHashCode() {
        return blockHeader.getHashCode();
    }

    @Override
    public void write(OutputStream stream) throws IOException, AdeniumException {
        blockHeader.write(stream);
        VarInt.writeCompactUInt32(transactions.size(), false, stream);
        for (Transaction transaction : transactions)
        {
            // use serialize here to write transaction serial id
            transaction.serialize(stream);
        }
    }

    @Override
    public void read(InputStream stream) throws IOException, AdeniumException {
        blockHeader.read(stream);
        int length = VarInt.readCompactUInt32(false, stream);

        for (int i = 0; i < length; i ++)
        {
            transactions.add(Context.getInstance().getSerialFactory().fromStream(stream));
        }
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
        return (Type) new Block();
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(Block.class);
    }

    public Transaction getCoinbase()
    {
        Iterator<Transaction> transactions = this.transactions.iterator();
        if (transactions.hasNext())
        {
            return transactions.next();
        }

        return null;
    }

    public BigInteger getWork() {
        return blockHeader.getWork();
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
    }

    public int getTransactionCount() {
        return transactions.size();
    }

    public void removeLastTransaction() {
        Iterator<Transaction> transactions = this.transactions.iterator();
        if (transactions.hasNext())
        {
            transactions.next();

            if (!transactions.hasNext()) {
                transactions.remove();
            }
        }
    }

    @Override
    public Iterator<Transaction> iterator() {
        return transactions.iterator();
    }

    public long getFees() {
        long fees = 0L;

        for (Transaction transaction : transactions) {
            fees += transaction.getTransactionFee();
        }

        return fees;
    }

    public Set<byte[]> getPrunedTransactions() {
        Set<byte[]> pruned = new LinkedHashSet<>();
        for (Transaction transaction : transactions) {
            pruned.add(transaction.getHash());
        }

        return pruned;
    }

    public int getEventCount() {
        return 0;
    }

    public long getTotalValue() {
        return 0;
    }

    public Set<Transaction> getTransactions() {
        return transactions;
    }

    public byte[] getSerializedTransactions() throws IOException, AdeniumException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (Transaction transaction : transactions) {
            transaction.serialize(outputStream);
        }

        return outputStream.toByteArray();
    }

    public byte[] getSerializedEvents() throws IOException, AdeniumException {
        return null;
    }

    public void write(OutputStream outputStream, boolean writeLocally) throws IOException, AdeniumException {
        write(outputStream);
        if (writeLocally) {
            // start writing events
            outputStream.write(stateChange.getTransactionMerkleRoot());
            outputStream.write(stateChange.getTransactionEventMerkleRoot());
            VarInt.writeCompactUInt32(stateChange.getTransactionEvents().size(), false, outputStream);
            for (Event event : stateChange.getTransactionEvents()) {
                event.serialize(outputStream);
            }
        }
    }

    public PrunedBlock getPruned() {
        return new PrunedBlock(getBlockHeader(), getStateChange().getTransactionIds(), getStateChange().getTransactionEventIds());
    }

    public void setNonce(int nonce) {
        blockHeader.setNonce(nonce);
    }

    protected void setMerkleRoot(byte[] merkleRoot) {
        blockHeader.setMerkleRoot(merkleRoot);
    }

    public void setParent(byte[] parent) {
        blockHeader.setParent(parent);
    }

    public void setBits(int bits) {
        blockHeader.setBits(bits);
    }

    public int getVersion() {
        return blockHeader.getVersion();
    }

    public int getTimestamp() {
        return blockHeader.getTimestamp();
    }

    public byte[] getParentHash() {
        return blockHeader.getParentHash();
    }

    public byte[] getMerkleRoot() {
        return blockHeader.getMerkleRoot();
    }

    public int getNonce() {
        return blockHeader.getNonce();
    }

    public int getBits() {
        return blockHeader.getBits();
    }

    public byte[] getTargetBytes() {
        return blockHeader.getTargetBytes();
    }

    public BigInteger getTargetInteger() {
        return blockHeader.getTargetInteger();
    }

    public byte[] getHeaderBytes() {
        return blockHeader.asByteArray();
    }

    public boolean verifyProofOfWork() {
        return blockHeader.verifyProofOfWork();
    }
}
