package io.adenium.papaya.runtime;

import io.adenium.exceptions.PapayaException;

public interface PapayaCallable {
    public static final PapayaCallable Default = scope -> {};
    public void call(Scope scope) throws PapayaException;
}
