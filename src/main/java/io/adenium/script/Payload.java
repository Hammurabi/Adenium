package io.adenium.script;

import io.adenium.exceptions.ContractException;
import io.adenium.exceptions.WolkenException;
import io.adenium.serialization.SerializableI;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class Payload extends SerializableI {
    public Payload() {
    }

    /* this will call any function marked as "entrypoint" in payload */
    public abstract void entryPoint(Invoker invoker) throws ContractException;

    public abstract int getVersion();

    public abstract void writePayload(OutputStream stream) throws IOException, WolkenException;
    public abstract void readPayload(InputStream stream) throws IOException, WolkenException;

    @Override
    public final void write(OutputStream stream) throws IOException, WolkenException {
        writePayload(stream);
    }

    @Override
    public final void read(InputStream stream) throws IOException, WolkenException {
        readPayload(stream);
    }
}
