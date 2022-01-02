package io.adenium.core;

import io.adenium.exceptions.AdeniumException;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.VarInt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

public class PrunedBlock extends SerializableI {
    private final BlockHeader           blockHeader;
    private final Collection<byte[]>    transactions;
    private final Collection<byte[]>    events;

    public PrunedBlock() {
        this(new BlockHeader(), new LinkedList<>(), new LinkedList<>());
    }

    public PrunedBlock(BlockHeader blockHeader, Queue<byte[]> transactions, Queue<byte[]> events) {
        this.blockHeader = blockHeader;
        this.transactions = transactions;
        this.events = events;
    }

    public Collection<byte[]> getTransactions() {
        return transactions;
    }

    public Collection<byte[]> getEvents() {
        return events;
    }

    public boolean containsTransaction(byte transactionId[]) {
        return transactions.contains(transactionId);
    }

    public boolean containsEvent(byte eventId[]) {
        return events.contains(eventId);
    }

    @Override
    public void write(OutputStream stream) throws IOException, AdeniumException {
        blockHeader.write(stream);
        VarInt.writeCompactUInt32(transactions.size(), false, stream);
        VarInt.writeCompactUInt32(events.size(), false, stream);
        for (byte[] transaction : transactions) {
            stream.write(transaction);
        }
        for (byte[] event : events) {
            stream.write(event);
        }
    }

    @Override
    public void read(InputStream stream) throws IOException, AdeniumException {
        blockHeader.read(stream);
        int numTransactions = VarInt.readCompactUInt32(false, stream);
        int numEvents       = VarInt.readCompactUInt32(false, stream);
        byte buffer[]       = new byte[32];

        for (int i = 0; i < numTransactions; i ++) {
            checkFullyRead(stream.read(buffer), buffer.length);
            transactions.add(Arrays.copyOf(buffer, 32));
        }

        for (int i = 0; i < numEvents; i ++) {
            checkFullyRead(stream.read(buffer), buffer.length);
            events.add(Arrays.copyOf(buffer, 32));
        }
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
        return (Type) new PrunedBlock(new BlockHeader(), new LinkedList<>(), new LinkedList<>());
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(PrunedBlock.class);
    }
}
