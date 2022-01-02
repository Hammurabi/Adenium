package io.adenium.network.messages;

import io.adenium.core.Context;
import io.adenium.exceptions.WolkenException;
import io.adenium.network.*;
import org.wolkenproject.network.*;
import io.adenium.serialization.SerializableI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;

public class VerackMessage extends Message {
    private VersionInformation versionInformation;

    public VerackMessage() throws UnknownHostException {
        this(0, new VersionInformation());
    }

    public VerackMessage(int version, VersionInformation versionInformation) {
        super(version, Flags.Notify);
        this.versionInformation = versionInformation;
    }

    @Override
    public void executePayload(Server server, Node node) {
        node.setVersionInfo(versionInformation);
        Context.getInstance().getIpAddressList().send(node);

        node.sendMessage(new RequestInv(Context.getInstance().getContextParams().getVersion()));
    }

    @Override
    public void writeContents(OutputStream stream) throws IOException, WolkenException {
        versionInformation.write(stream);
    }

    @Override
    public void readContents(InputStream stream) throws IOException, WolkenException {
        versionInformation.read(stream);
    }

    @Override
    public <Type> Type getPayload() {
        return (Type) versionInformation;
    }

    @Override
    public ResponseMetadata getResponseMetadata() {
        return null;
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws WolkenException {
        try {
            return (Type) new VerackMessage(0, new VersionInformation());
        } catch (UnknownHostException e) {
            return null;
        }
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(VerackMessage.class);
    }
}
