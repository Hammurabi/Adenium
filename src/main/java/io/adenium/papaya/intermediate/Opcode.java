package io.adenium.papaya.intermediate;

import io.adenium.exceptions.PapayaException;
import io.adenium.exceptions.WolkenException;
import io.adenium.papaya.runtime.Scope;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Opcode {
    void execute(Scope scope) throws PapayaException;
    default void read(InputStream stream) throws IOException, WolkenException {}
    default void write(OutputStream stream) throws IOException, WolkenException {}
}
