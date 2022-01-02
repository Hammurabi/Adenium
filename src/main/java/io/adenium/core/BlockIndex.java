package io.adenium.core;

import io.adenium.core.transactions.Transaction;
import io.adenium.encoders.Base16;
import io.adenium.exceptions.AdeniumException;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.ChainMath;
import io.adenium.utils.HashUtil;
import io.adenium.utils.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

public class BlockIndex extends SerializableI implements Comparable<BlockIndex> {
    private Block       block;
    private byte        hash[];
    private BigInteger  chainWork;
    private int         height;
    private long        sequenceId;

    public BlockIndex() {
        this(new Block(), BigInteger.ZERO, 0);
    }

    public BlockIndex(Block block, BigInteger chainWork, int height) {
        this.block      = block;
        this.hash       = block.getHashCode();
        this.chainWork  = chainWork;
        this.height     = height;
        this.sequenceId = 0;
    }

    public BlockIndex(Block block, BlockMetadata metadata) {
        this.block = block;
        this.hash  = block.getHashCode();
        this.chainWork = metadata.getPreviousChainWork();
        this.height = metadata.getHeight();
        this.sequenceId = 0;
    }

    public void setSequenceId(long sequenceId) {
        this.sequenceId = sequenceId;
    }

    public Block getBlock() {
        return block;
    }

    public BigInteger getTotalChainWork() {
        return chainWork.add(block.getWork());
    }

    public BigInteger getPreviousChainWork() {
        return chainWork;
    }

    public int getHeight() {
        return height;
    }

    public BlockIndex generateNextBlock() {
        Block nextBlock         = new Block(getHash(), getBlock().getBits());
        int bits                = ChainMath.calculateNewTarget(nextBlock.getBlockHeader(), height + 1);
        return new BlockIndex(nextBlock, getTotalChainWork(), height + 1);
    }

    @Override
    public void write(OutputStream stream) throws IOException, AdeniumException {
        block.write(stream);
        byte chainWork[] = this.chainWork.toByteArray();
        stream.write(chainWork.length);
        stream.write(chainWork);
        Utils.writeInt(height, stream);
    }

    @Override
    public void read(InputStream stream) throws IOException, AdeniumException {
        block.read(stream);
        byte length = (byte) stream.read();
        byte chainWork[] = new byte[length];
        if (chainWork.length == 0) {
            this.chainWork = BigInteger.ZERO;
        } else {
            stream.read(chainWork);
            this.chainWork = new BigInteger(chainWork);
        }

        height = Utils.readInt(stream);

        hash = block.getHashCode();
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
        return (Type) new BlockIndex();
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(BlockIndex.class);
    }

    public void recalculateChainWork() throws AdeniumException {
        recalculateChainWork(this);
    }

    public static void recalculateChainWork(BlockIndex index) throws AdeniumException {
        while (index != null) {
            BlockIndex previous = index.previousBlock();

            BigInteger oldChainWork = index.chainWork;
            BigInteger newChainWork = oldChainWork;

            if (previous != null) {
                newChainWork  = previous.getTotalChainWork();
            } else {
                newChainWork  = BigInteger.ZERO;
            }

            if (newChainWork.compareTo(oldChainWork) == 0) {
                return;
            } else {
                // set the new chain work
                index.chainWork = newChainWork;

                // save changes made to the block index
                Context.getInstance().getDatabase().storeBlock(index.getHeight(), index);
            }

            if (index.hasNext()) {
                index = index.next();
            }
        }
    }

    public boolean hasNext() {
        return Context.getInstance().getDatabase().checkBlockExists(getHeight() + 1);
    }

    public boolean hasPrev() {
        return Context.getInstance().getDatabase().checkBlockExists(getHeight() - 1);
    }

    public BlockIndex next() {
        return Context.getInstance().getDatabase().findBlock(getHeight() + 1);
    }

    public BlockIndex previousBlock() {
        if (height == 0) {
            return null;
        }

        return Context.getInstance().getDatabase().findBlock(getHeight() - 1);
    }

    @Override
    public int compareTo(BlockIndex other) {
        int compare = getTotalChainWork().compareTo(other.getTotalChainWork());

        if (compare > 0) {
            return -1;
        }

        if (compare < 0) {
            return 1;
        }

        if (getSequenceId() < other.getSequenceId()) {
            return -1;
        }

        if (getSequenceId() > other.getSequenceId()) {
            return 1;
        }

        if (getBlock().getTransactionCount() > other.getBlock().getTransactionCount()) {
            return 1;
        }

        if (getBlock().getTransactionCount() < other.getBlock().getTransactionCount()) {
            return -1;
        }

        return -1;
    }

    public long getSequenceId() {
        return sequenceId;
    }

    @Override
    public String toString() {
        return "{" +
                "block=" + Base16.encode(block.getHashCode()) +
                ", chainWork=" + getTotalChainWork() +
                ", height=" + height +
                ", sequenceId=" + sequenceId +
                '}';
    }

    // always use this method when available
    // getBlock().getHashCode() is too expensive
    public byte[] getHash() {
        return hash;
    }

    @Override
    public byte[] checksum() {
        return HashUtil.hash160(getHash());
    }

    public BlockStateChangeResult getStateChange() {
        return getBlock().getStateChange();
    }

    /**
        @param txList include/exclude transaction list
        @param txHash if true then transactions will be replaced by their hashes
        @param evList include/exclude event list
        @param evHash if true then events will be replaced by their hashes
        @param txEvt if true then events will be included with their parent transaction,
                      otherwise they will be included in the 'state change'
     **/
    public JSONObject toJson(boolean txList, boolean txHash, boolean evList, boolean evHash, boolean txEvt) {
        JSONObject block    = new JSONObject();
        JSONObject header   = new JSONObject();
        JSONArray body      = new JSONArray();
        JSONArray state     = new JSONArray();

        header.put("version", getBlock().getVersion());
        header.put("timestamp", getBlock().getTimestamp());
        header.put("parentHash", Base16.encode(getBlock().getParentHash()));
        header.put("merkleRoot", Base16.encode(getBlock().getMerkleRoot()));
        header.put("bits", Base16.encode(Utils.takeApart(getBlock().getBits())));
        header.put("nonce", getBlock().getNonce());

        block.put("hash", Base16.encode(getHash()));
        block.put("header", header);

        if (txList) {
            int index = 0;
            for (Transaction transaction : getBlock()) {
                body.put(index ++, txHash ? Base16.encode(transaction.getHash()) : transaction.toJson(txEvt, evHash));
            }
            block.put("content", body);
        }

        if (!txEvt && evList) {
            int index = 0;
            for (Event event : getStateChange()) {
                state.put(index ++, evHash ? Base16.encode(event.eventId()) : event.toJson());
            }
            block.put("stateChange", state);
        }

        return block;
    }

    public void build() throws AdeniumException {
        getBlock().build(getHeight());
    }

    public byte[] calcHash() {
        hash = getBlock().getHashCode();
        return hash;
    }

    public PrunedBlock getPruned() throws AdeniumException {
        return getBlock().getPruned();
    }

    public BlockMetadata getMetadata() throws AdeniumException {
        return new BlockMetadata(getBlock().getBlockHeader(), 0, getHeight(), getBlock().getTransactionCount(), getBlock().getEventCount(), getTotalValue(), getFees(), getPreviousChainWork());
    }

    private long getFees() {
        return block.getFees();
    }

    private long getTotalValue() {
        return block.getTotalValue();
    }

    public BlockHeader getHeader() {
        return block.getBlockHeader();
    }

    public void applyStateChange() {
        getStateChange().apply();
    }

    public void undoStateChange() {
        getStateChange().undo();
    }
}
