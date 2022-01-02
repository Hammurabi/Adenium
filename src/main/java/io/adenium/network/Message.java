package io.adenium.network;

import io.adenium.exceptions.WolkenException;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.HashUtil;
import io.adenium.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;

public abstract class Message extends SerializableI {
    public static final int UniqueIdentifierLength = 20;

    public static final class Flags {
        public static final int
                None = 0,
                Notify = 1,
                Request = 2,
                Response = 4;
    }

    private int     version;
    private int     flags;
    private long    nonce;

    public Message(int version, int flags) {
        this(version, flags, new SecureRandom().nextLong());
    }

    public Message(int version, int flags, long nonce) {
        this.version    = version;
        this.flags      = flags;
        this.nonce      = nonce;
    }

    public abstract void executePayload(Server server, Node node);

    // called after a message is sent
    public void onSend(Node node) {
    }

    public void writeHeader(OutputStream stream) throws IOException, WolkenException {
        Utils.writeInt(version, stream);
        Utils.writeInt(flags, stream);
        stream.flush();
    }

    public void readHeader(InputStream stream) throws IOException, WolkenException {
        version = Utils.readInt(stream);
        flags = Utils.readInt(stream);
    }

    @Override
    public void write(OutputStream stream) throws IOException, WolkenException {
        writeHeader(stream);
        writeContents(stream);
    }

    @Override
    public void read(InputStream stream) throws IOException, WolkenException {
        readHeader(stream);
        readContents(stream);
    }

    public byte[] getUniqueMessageIdentifier() {
        return HashUtil.hash160(asByteArray());
    }

    public int getVersion() {
        return version;
    }

    public int getFlags() {
        return flags;
    }

//    public long getNonce() {
//        return nonce;
//    }

    public abstract void writeContents(OutputStream stream) throws IOException, WolkenException;
    public abstract void readContents(InputStream stream) throws IOException, WolkenException;

    public abstract <Type> Type getPayload();
    public abstract ResponseMetadata getResponseMetadata();
}