package io.adenium.network.messages;

import io.adenium.core.Context;
import io.adenium.exceptions.AdeniumException;
import io.adenium.network.Message;
import io.adenium.network.Node;
import io.adenium.network.ResponseMetadata;
import io.adenium.network.Server;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.Logger;
import io.adenium.utils.VarInt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CheckoutMessage extends Message {
    public static final class Reason {
        public static final int
            None = 0,
            SelfConnect = 1;
    }

    private int reason;

    public CheckoutMessage(int reason) {
        super(Flags.Notify, Context.getInstance().getContextParams().getVersion());
        this.reason = reason;
    }

    @Override
    public void executePayload(Server server, Node node) {
        Logger.alert("node ${n} requested to disconnect for reason ${r}", Logger.Levels.AlertMessage, node.getInetAddress(), reason);

        try {
            node.close();
        } catch (IOException e) {
            Logger.alert("could not disconnect from node properly.", Logger.Levels.AlertMessage);
            e.printStackTrace();
        }

        // this check should happen but it's not crucial
//        if (reason == Reason.SelfConnect) {
            // there is no version info if "checkout" is sent
//            if (!node.getVersionInfo().isSelfConnection(server.getNonce())) {
//                node.increaseErrors(1);
//            }
//        }
    }

    @Override
    public void onSend(Node node) {
        super.onSend(node);
        try {
            Logger.alert("connection termination requested ${r}.", reason);
            node.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeContents(OutputStream stream) throws IOException, AdeniumException {
        VarInt.writeCompactUInt32(reason, false, stream);
    }

    @Override
    public void readContents(InputStream stream) throws IOException, AdeniumException {
        reason = VarInt.readCompactUInt32(false, stream);
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
        return (Type) new CheckoutMessage(0);
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(CheckoutMessage.class);
    }
}
