package io.adenium.network.messages;

import io.adenium.core.Context;
import io.adenium.exceptions.AdeniumException;
import io.adenium.network.Message;
import io.adenium.network.Node;
import io.adenium.network.ResponseMetadata;
import io.adenium.network.Server;
import io.adenium.serialization.SerializableI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RequestInv extends Message {
    public RequestInv(int version) {
        super(version, Flags.Notify);
    }

    @Override
    public void executePayload(Server server, Node node) {
        node.sendMessage(new Inv(Context.getInstance().getContextParams().getVersion(), Inv.Type.Block, Context.getInstance().getBlockChain().getBestBlock().getHash()));
        node.sendMessage(new Inv(Context.getInstance().getContextParams().getVersion(), Inv.Type.Transaction, Context.getInstance().getTransactionPool().getHeadOfQueue()));
    }

    @Override
    public void writeContents(OutputStream stream) throws IOException, AdeniumException {
    }

    @Override
    public void readContents(InputStream stream) throws IOException, AdeniumException {
    }

    @Override
    public <Type> Type getPayload() {
        return null;
    }

    @Override
    public ResponseMetadata getResponseMetadata() {
        return null;
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
        return (Type) new RequestInv(0);
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(RequestInv.class);
    }
}
