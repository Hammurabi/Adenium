package io.adenium.core.events;

import org.json.JSONObject;
import io.adenium.core.Context;
import io.adenium.core.Event;
import io.adenium.encoders.Base58;
import io.adenium.exceptions.WolkenException;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.Utils;
import io.adenium.utils.VarInt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RegisterAliasEvent extends Event {
    private byte    address[];
    private long    alias;

    public RegisterAliasEvent(byte[] address, long alias) {
        super();
    }

    @Override
    public void apply() {
        Context.getInstance().getDatabase().registerAlias(address, alias);
    }

    @Override
    public void undo() {
        Context.getInstance().getDatabase().removeAlias(alias);
    }

    @Override
    public byte[] getEventBytes() {
        return Utils.concatenate("Register Alias".getBytes(), address, Utils.takeApartLong(alias));
    }

    @Override
    public JSONObject toJson() {
        return new JSONObject().put("event", this.getClass().getName()).put("address", Base58.encode(address)).put("alias", alias);
    }

    public long getAlias() {
        return alias;
    }

    @Override
    public void write(OutputStream stream) throws IOException, WolkenException {
        stream.write(address);
        VarInt.writeCompactUInt64(alias, false, stream);
    }

    @Override
    public void read(InputStream stream) throws IOException, WolkenException {
        checkFullyRead(stream.read(address), address.length);
        alias = VarInt.readCompactUInt64(false, stream);
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws WolkenException {
        return (Type) new RegisterAliasEvent(new byte[address.length], 0);
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(RegisterAliasEvent.class);
    }
}
