package io.adenium.core.events;

import org.json.JSONObject;
import io.adenium.core.Event;
import io.adenium.exceptions.AdeniumException;
import io.adenium.serialization.SerializableI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DestroyContractEvent extends Event {
    public DestroyContractEvent(byte[] contractId) {
        super();
    }

    @Override
    public void apply() {
    }

    @Override
    public void undo() {
    }

    @Override
    public byte[] getEventBytes() {
        return new byte[0];
    }

    @Override
    public JSONObject toJson() {
        return null;
    }

    @Override
    public void write(OutputStream stream) throws IOException, AdeniumException {
    }

    @Override
    public void read(InputStream stream) throws IOException, AdeniumException {
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
        return null;
    }

    @Override
    public int getSerialNumber() {
        return 0;
    }
}
