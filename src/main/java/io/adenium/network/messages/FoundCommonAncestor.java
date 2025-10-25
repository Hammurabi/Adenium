package io.adenium.network.messages;

import io.adenium.core.Block;
import io.adenium.core.Context;
import io.adenium.exceptions.AdeniumException;
import io.adenium.network.Message;
import io.adenium.network.Node;
import io.adenium.network.Server;
import io.adenium.serialization.SerializableI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FoundCommonAncestor extends ResponseMessage {
    private byte commonAncestor[];

    public FoundCommonAncestor(byte commonAncestor[], byte[] uniqueMessageIdentifier) {
        super(Context.getInstance().getContextParams().getVersion(), uniqueMessageIdentifier);
        this.commonAncestor = commonAncestor;
    }

    @Override
    public void writeContents(OutputStream stream) throws IOException, AdeniumException {
        stream.write(commonAncestor != null ? 1 : 0);
        if (commonAncestor != null) {
            stream.write(commonAncestor);
        }
    }

    @Override
    public void readContents(InputStream stream) throws IOException, AdeniumException {
        int bool = stream.read();
        if (bool == 1) {
            stream.read(commonAncestor);
        } else {
            commonAncestor = null;
        }
    }

    @Override
    public <Type> Type getPayload() {
        return (Type) commonAncestor;
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
        return (Type) new FoundCommonAncestor(new byte[Block.UniqueIdentifierLength], new byte[Message.UniqueIdentifierLength]);
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(FoundCommonAncestor.class);
    }

    @Override
    public void execute(Server server, Node node) {
    }
}
