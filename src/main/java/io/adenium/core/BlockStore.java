package io.adenium.core;

import io.adenium.fastnio.Buffer;

public class BlockStore {
    private static final int MaxLength = 128_000_000;
    private final Buffer data;

    public BlockStore() {
        this(0, 0, 0, 0, 0, 0);
    }

    public BlockStore(byte data[]) {
        this.data = Buffer.wrap(data);
    }

    public BlockStore(int numberOfBlocks, int lowestHeightBlock, int highestBlock, int lowestTimestampBlock, int highestTimestampBlock, int index) {
        int maxBlocks = MaxLength / Context.getInstance().getContextParams().getMaxBlockSize();
        this.data = Buffer.createBuffer(maxBlocks * 8 + 22);
        this.data.putInt(0, numberOfBlocks);
        this.data.putInt(4, lowestHeightBlock);
        this.data.putInt(8, highestBlock);
        this.data.putInt(12, lowestTimestampBlock);
        this.data.putInt(16, highestTimestampBlock);
        this.data.putInt(20, index);
    }

    public int getBlockLocation(int block) {
        return data.getInt(block * 8 + 24);
    }

    public int getBlockSize(int block) {
        return data.getInt(block * 8 + 28);
    }

    public boolean hasSpaceRemaining(int length) {
        return (getIndex() + length) < MaxLength;
    }

    public int getNumberOfBlocks() {
        return data.getInt(0);
    }

    public int getLowestBlock() {
        return data.getInt(4);
    }

    public int getHighestBlock() {
        return data.getInt(8);
    }

    public int getLowestTimestamp() {
        return data.getInt(12);
    }

    public int getHighestTimestamp() {
        return data.getInt(16);
    }

    public int getIndex() {
        return data.getInt(20);
    }
}
