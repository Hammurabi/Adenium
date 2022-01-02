package io.adenium.core.assets;

import io.adenium.serialization.SerializableI;
import io.adenium.core.Context;
import io.adenium.exceptions.AdeniumException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

public class NonFungibleToken extends Asset {
    private boolean isTransferable;

    public NonFungibleToken() {
        this(DefaultUUID, false);
    }

    public NonFungibleToken(byte[] uuid, boolean isTransferable) {
        super(uuid);
        this.isTransferable = isTransferable;
    }

    @Override
    public void writeContent(OutputStream stream) throws IOException, AdeniumException {
        stream.write(isTransferable ? 1 : 0);
    }

    @Override
    public void readContent(InputStream stream) throws IOException, AdeniumException {
        isTransferable = checkNotEOF(stream.read()) == 1;
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
        return (Type) new NonFungibleToken(new byte[UniqueIdentifierLength], false);
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(NonFungibleToken.class);
    }

    @Override
    public boolean isTransferable() {
        return isTransferable;
    }

    @Override
    public boolean isFungible() {
        return false;
    }

    @Override
    public BigInteger getTotalSupply() {
        return BigInteger.ONE;
    }
}
