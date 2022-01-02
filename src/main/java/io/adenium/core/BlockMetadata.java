package io.adenium.core;

import io.adenium.exceptions.AdeniumException;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.Utils;
import io.adenium.utils.VarInt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

public class BlockMetadata extends SerializableI {
    private BlockHeader blockHeader;
    private int flags;
    private int height;
    private int transactionCount;
    private int eventCount;
    private long totalValue;
    private long fees;
    private BigInteger chainWork;

    public BlockMetadata() {
        this(new BlockHeader(), 0, 0, 0, 0, 0, 0, BigInteger.ZERO);
    }

    public BlockMetadata(BlockHeader blockHeader, int flags, int height, int transactionCount, int eventCount, long totalValue, long fees, BigInteger chainWork) {
        this.blockHeader = blockHeader;
        this.flags = flags;
        this.height = height;
        this.transactionCount = transactionCount;
        this.eventCount = eventCount;
        this.totalValue = totalValue;
        this.fees = fees;
        this.chainWork = chainWork;
    }

    public BlockHeader getBlockHeader() {
        return blockHeader;
    }

    public int getFlags() {
        return flags;
    }

    public int getHeight() {
        return height;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public int getEventCount() {
        return eventCount;
    }

    public long getTotalValue() {
        return totalValue;
    }

    public long getFees() {
        return fees;
    }

    public BigInteger getPreviousChainWork() {
        return chainWork;
    }

    @Override
    public void write(OutputStream stream) throws IOException, AdeniumException {
        getBlockHeader().write(stream);
        Utils.writeInt(getFlags(), stream);
        VarInt.writeCompactUInt32(getHeight(), false, stream);
        VarInt.writeCompactUInt32(getTransactionCount(), false, stream);
        VarInt.writeCompactUInt32(getEventCount(), false, stream);
        VarInt.writeCompactUInt64(getTotalValue(), false, stream);
        VarInt.writeCompactUInt64(getFees(), false, stream);
        VarInt.writeCompactUint256(getPreviousChainWork(), true, stream);
    }

    @Override
    public void read(InputStream stream) throws IOException, AdeniumException {
        getBlockHeader().read(stream);
        flags = Utils.readInt(stream);
        height = VarInt.readCompactUInt32(false, stream);
        transactionCount = VarInt.readCompactUInt32(false, stream);
        eventCount = VarInt.readCompactUInt32(false, stream);
        totalValue = VarInt.readCompactUInt64(false, stream);
        fees = VarInt.readCompactUInt64(false, stream);
        chainWork = VarInt.readCompactUint256(true, stream);
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
        return (Type) new BlockMetadata();
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(BlockMetadata.class);
    }

    public boolean isStale() {
        return (flags & Flags.StaleBlock) == Flags.StaleBlock;
    }

    public void setStale(boolean stale) {
        if (stale) {
            flags = flags | Flags.StaleBlock;
        } else {
            flags = flags & ~Flags.StaleBlock;
        }
    }

    public static final class Flags {
        public static final int
                MainChain  = 0,
                StaleBlock = 1,
                None = 0;
    }
}
