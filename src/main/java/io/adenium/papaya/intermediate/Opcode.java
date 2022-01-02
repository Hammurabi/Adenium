package io.adenium.papaya.intermediate;

import io.adenium.exceptions.PapayaException;
import io.adenium.exceptions.AdeniumException;
import io.adenium.papaya.runtime.Scope;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Opcode {
    void execute(Scope scope) throws PapayaException;
    default void read(InputStream stream) throws IOException, AdeniumException {}
    default void write(OutputStream stream) throws IOException, AdeniumException {}
}
