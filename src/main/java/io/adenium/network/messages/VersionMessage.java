package io.adenium.network.messages;

import io.adenium.core.Context;
import io.adenium.exceptions.AdeniumException;
import io.adenium.network.*;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class VersionMessage extends Message {
    private VersionInformation versionInformation;

    public VersionMessage() throws UnknownHostException {
        this(0, new VersionInformation(0, 0, 0, new NetAddress(InetAddress.getLocalHost(), 0, 0), new NetAddress(InetAddress.getLocalHost(), 0, 0), 0, new byte[20]));
    }

    public VersionMessage(int version, VersionInformation versionInformation) {
        super(version, Flags.Notify);
        this.versionInformation = versionInformation;
    }

    @Override
    public void executePayload(Server server, Node node) {
        node.setVersionInfo(versionInformation);
        Logger.notify("received version info ${i}", Logger.Levels.NotificationMessage, versionInformation);

        if (!Context.getInstance().getContextParams().isVersionCompatible(versionInformation.getVersion(), Context.getInstance().getContextParams().getVersion())) {
            // send bye message.
            Logger.notify("terminating connection.. (incompatible versions)", Logger.Levels.NotificationMessage);
            node.sendMessage(new CheckoutMessage(CheckoutMessage.Reason.SelfConnect));
        } else if (versionInformation.isSelfConnection(server.getNonce())) {
            // this is a self connection, we must terminate it
            Logger.notify("terminating self connection..", Logger.Levels.NotificationMessage);
            node.sendMessage(new CheckoutMessage(CheckoutMessage.Reason.SelfConnect));
        } else {
            Logger.notify("sending verack..", Logger.Levels.NotificationMessage);
            // send verack
            node.sendMessage(new VerackMessage(Context.getInstance().getContextParams().getVersion(), new VersionInformation(
                    Context.getInstance().getContextParams().getVersion(),
                    Context.getInstance().getContextParams().getServices(),
                    System.currentTimeMillis(),
                    server.getNetAddress(),
                    node.getNetAddress(),
                    Context.getInstance().getBlockChain().getHeight(),
                    server.getNonce()
            )));

            Context.getInstance().getIpAddressList().send(node);
            node.sendMessage(new RequestInv(Context.getInstance().getContextParams().getVersion()));
        }
    }

    @Override
    public void writeContents(OutputStream stream) throws IOException, AdeniumException {
        versionInformation.write(stream);
    }

    @Override
    public void readContents(InputStream stream) throws IOException, AdeniumException {
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
    public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
        try {
            return (Type) new VersionMessage();
        } catch (UnknownHostException e) {
            throw new AdeniumException(e);
        }
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(VersionMessage.class);
    }

    public VersionInformation getVersionInformation()
    {
        return versionInformation;
    }
}