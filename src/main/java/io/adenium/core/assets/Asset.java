package io.adenium.core.assets;

import io.adenium.serialization.SerializableI;
import io.adenium.serialization.SerializationFactory;
import io.adenium.core.Address;
import io.adenium.exceptions.WolkenException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

public abstract class Asset extends SerializableI {
    public final static int     UniqueIdentifierLength = 20;
    public final static byte    DefaultUUID[] = new byte[UniqueIdentifierLength];
    // 20 byte unique identifier (hash160 of constructor contract/transaction).
    private final byte          uuid[];

    // default constructor
    public Asset(byte uuid[]) {
        this.uuid       = uuid;
    }

    public byte[] getUUID() {
        return uuid;
    }

    @Override
    public void write(OutputStream stream) throws IOException, WolkenException {
        stream.write(uuid);
    }

    @Override
    public void read(InputStream stream) throws IOException, WolkenException {
        checkFullyRead(stream.read(uuid), Address.RawLength);
    }

    public abstract boolean isTransferable();
    public abstract boolean isFungible();
    public abstract BigInteger getTotalSupply();

    public abstract void writeContent(OutputStream stream) throws IOException, WolkenException;
    public abstract void readContent(InputStream stream) throws IOException, WolkenException;

    public static void register(SerializationFactory serializationFactory) {
        serializationFactory.registerClass(NonFungibleToken.class, new NonFungibleToken());
    }
}
