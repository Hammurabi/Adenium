package io.adenium.network.messages;

import io.adenium.core.Context;
import io.adenium.exceptions.AdeniumException;
import io.adenium.network.Node;
import io.adenium.network.Server;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FailedToRespondMessage extends ResponseMessage {
    public static final class ReasonFlags {
        public static final long
        NoReasonSpecified = 0,
        CouldNotFindRequestedData = 1 << 1;
    }

    private long reasonFlags;

    public FailedToRespondMessage(int version, long reasonFlags, byte[] uniqueMessageIdentifier) {
        super(version, uniqueMessageIdentifier);
        this.reasonFlags = reasonFlags;
    }

    @Override
    public void writeContents(OutputStream stream) throws IOException, AdeniumException {
        Utils.writeLong(reasonFlags, stream);
    }

    @Override
    public void readContents(InputStream stream) throws IOException, AdeniumException {
        byte buffer[] = new byte[8];
        reasonFlags = Utils.makeLong(buffer);
    }

    @Override
    public <Type> Type getPayload() {
        return (Type) (Long) reasonFlags;
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
        return (Type) new FailedToRespondMessage(0, 0, new byte[UniqueIdentifierLength]);
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(FailedToRespondMessage.class);
    }

    @Override
    public void execute(Server server, Node node) {
    }
}
