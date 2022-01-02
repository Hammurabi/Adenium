package io.adenium.network.messages;

import io.adenium.core.Context;
import io.adenium.core.transactions.Transaction;
import io.adenium.exceptions.WolkenException;
import io.adenium.network.Node;
import io.adenium.network.Server;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.VarInt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class TransactionList extends ResponseMessage {
    private Set<Transaction>   transactions;

    public TransactionList(int version, Collection<Transaction> transactions, byte[] uniqueMessageIdentifier) {
        super(version, uniqueMessageIdentifier);
        this.transactions   = new LinkedHashSet<>(transactions);
    }

    @Override
    public void execute(Server server, Node node) {
    }

    @Override
    public void writeContents(OutputStream stream) throws IOException, WolkenException {
        VarInt.writeCompactUInt32(transactions.size(), false, stream);
        for (Transaction transaction : transactions)
        {
            transaction.write(stream);
        }
    }

    @Override
    public void readContents(InputStream stream) throws IOException, WolkenException {
        int length = VarInt.readCompactUInt32(false, stream);

        for (int i = 0; i < length; i++) {
            try {
                Transaction transaction = Context.getInstance().getSerialFactory().fromStream(Context.getInstance().getSerialFactory().getSerialNumber(Transaction.class), stream);
                transactions.add(transaction);
            } catch (WolkenException e) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public <Type> Type getPayload() {
        return (Type) transactions;
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws WolkenException {
        return (Type) new TransactionList(getVersion(), transactions, getUniqueMessageIdentifier());
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(TransactionList.class);
    }
}
