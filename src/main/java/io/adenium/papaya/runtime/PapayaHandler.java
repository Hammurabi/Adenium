package io.adenium.papaya.runtime;

import io.adenium.papaya.compiler.AccessModifier;
import io.adenium.papaya.compiler.Struct;
import io.adenium.exceptions.PapayaException;
import io.adenium.exceptions.PapayaIllegalAccessException;
import io.adenium.utils.ByteArray;

import java.math.BigInteger;
import java.util.Stack;

public abstract class PapayaHandler {
    private PapayaObject papayaObject;

    public PapayaHandler(PapayaObject papayaObject) {
        this.papayaObject = papayaObject;
    }

    public abstract void setMember(ByteArray memberId, PapayaHandler member, Stack<io.adenium.papaya.compiler.Struct> stackTrace) throws PapayaIllegalAccessException;
    public abstract PapayaHandler getMember(ByteArray memberId, Stack<io.adenium.papaya.compiler.Struct> stackTrace) throws PapayaIllegalAccessException;
    public abstract void call(Scope scope) throws PapayaException;

    public PapayaObject getPapayaObject() {
        return papayaObject;
    }

    public static final PapayaHandler doNothingHandler(PapayaObject object) {
        return new DefaultHandler(object);
    }

    public static final PapayaHandler readOnlyHandler(PapayaObject object) {
        return new ReadOnlyHandler(object);
    }

    public boolean asBool() {
        return papayaObject.asBool();
    }

    public BigInteger asInt() {
        return papayaObject.asInt();
    }

    public abstract PapayaHandler getAtIndex(int index) throws PapayaException;

    public abstract void setAtIndex(int index, PapayaHandler handler) throws PapayaException;

    public abstract void append(PapayaHandler handler) throws PapayaException;

    public abstract AccessModifier getModifier();

    public boolean isReadOnly() {
        return getModifier() == AccessModifier.ReadOnly;
    }

    public Struct getStructure() {
        return getPapayaObject().getStructure();
    }

    public void set(PapayaHandler object) {
        papayaObject = object.papayaObject;
    }
}
