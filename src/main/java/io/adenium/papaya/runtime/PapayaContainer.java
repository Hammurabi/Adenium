package io.adenium.papaya.runtime;

import io.adenium.exceptions.PapayaException;

import java.math.BigInteger;

public interface PapayaContainer {
    public PapayaObject getAtIndex(int index) throws PapayaException;
    public void setAtIndex(int index, PapayaHandler handler) throws PapayaException;
    public void append(PapayaHandler object);
    public BigInteger asInt();
    public BigInteger asSignedInt();
}
