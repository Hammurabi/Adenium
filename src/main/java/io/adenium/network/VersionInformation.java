package io.adenium.network;

import io.adenium.encoders.Base16;
import io.adenium.exceptions.AdeniumException;
import io.adenium.serialization.SerializableI;
import org.json.JSONObject;
import io.adenium.core.Context;
import io.adenium.utils.Utils;
import io.adenium.utils.VarInt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class VersionInformation extends SerializableI {
    public static final class Flags {
        public static final long
                AllServices = 0b11111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L;
    }

    private int version;
    private long services;
    private long timestamp;
    private NetAddress sender;
    private NetAddress receiver;
    private int blockHeight;
    private byte nonce[];

    public VersionInformation() throws UnknownHostException {
        this(0, 0, 0, new NetAddress(InetAddress.getLocalHost(), 0, 0), new NetAddress(InetAddress.getLocalHost(), 0, 0), 0, new byte[20]);
    }

    /**
     * @param version     client version
     * @param services    bitfield of services provided by this client
     * @param timestamp   unix timestamp of message creation time
     * @param sender      sender address
     * @param receiver    received address
     * @param blockHeight current block height
     */
    public VersionInformation(int version, long services, long timestamp, NetAddress sender, NetAddress receiver, int blockHeight, byte nonce[]) {
        this.version = version;
        this.services = services;
        this.timestamp = timestamp;
        this.sender = sender;
        this.receiver = receiver;
        this.blockHeight = blockHeight;
        this.nonce = nonce;
    }

    @Override
    public void write(OutputStream stream) throws IOException {
        VarInt.writeCompactUInt32(version, false, stream);
        Utils.writeLong(services, stream);
        Utils.writeLong(timestamp, stream);
        sender.write(stream);
        receiver.write(stream);
        Utils.writeInt(blockHeight, stream);
        stream.write(nonce);
    }

    @Override
    public void read(InputStream stream) throws IOException, AdeniumException {
        this.version = VarInt.readCompactUInt32(false, stream);
        this.services = Utils.readLong(stream);
        this.timestamp = Utils.readLong(stream);

        sender      = Context.getInstance().getSerialFactory().fromStream(Context.getInstance().getSerialFactory().getSerialNumber(NetAddress.class), stream);
        receiver    = Context.getInstance().getSerialFactory().fromStream(Context.getInstance().getSerialFactory().getSerialNumber(NetAddress.class), stream);

        this.blockHeight = Utils.readInt(stream);
        checkFullyRead(stream.read(nonce), 20);
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
        try {
            return (Type) new VersionInformation();
        } catch (UnknownHostException e) {
            throw new AdeniumException(e);
        }
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(VersionInformation.class);
    }

    public int getVersion() {
        return version;
    }

    public long getServices() {
        return services;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public NetAddress getSender() {
        return sender;
    }

    public NetAddress getReceiver() {
        return receiver;
    }

    public int getBlockHeight() {
        return blockHeight;
    }

    public boolean isSelfConnection(byte nonce[]) {
        return Arrays.equals(nonce, this.nonce);
    }

    @Override
    public String toString() {
        return "VersionInformation{" +
                "version=" + version +
                ", services=" + services +
                ", timestamp=" + timestamp +
                ", sender=" + sender +
                ", receiver=" + receiver +
                ", blockHeight=" + blockHeight +
                ", nonce=" + Arrays.toString(nonce) +
                '}';
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("version", version);
        json.put("services", services);
        json.put("timestamp", Utils.jsonDate(timestamp));
        json.put("blockHeight", blockHeight);
        json.put("nonce", Base16.encode(nonce));
        return json;
    }
}
