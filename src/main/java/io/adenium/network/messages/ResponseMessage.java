package io.adenium.network.messages;

import io.adenium.exceptions.AdeniumException;
import io.adenium.network.Message;
import io.adenium.network.Node;
import io.adenium.network.ResponseMetadata;
import io.adenium.network.Server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class ResponseMessage extends Message {
    private byte[] requester;

    public ResponseMessage(int version, byte uniqueMessageIdentifier[]) {
        super(version, Flags.Response);
        this.requester = uniqueMessageIdentifier;
    }

    @Override
    public void executePayload(Server server, Node node) {
        node.receiveResponse(this, requester);
        execute(server, node);
    }

    public abstract void execute(Server server, Node node);

    @Override
    public void writeHeader(OutputStream stream) throws IOException, AdeniumException {
        super.writeHeader(stream);
        stream.write(requester);
    }

    @Override
    public void readHeader(InputStream stream) throws IOException, AdeniumException {
        super.readHeader(stream);
        stream.read(requester);
    }

    protected byte[] getRequester() {
        return requester;
    }

    @Override
    public ResponseMetadata getResponseMetadata() {
        return null;
    }
}
