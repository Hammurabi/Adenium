package io.adenium.papaya.runtime;

import io.adenium.papaya.compiler.AccessModifier;
import io.adenium.papaya.compiler.Struct;
import io.adenium.exceptions.PapayaException;
import io.adenium.exceptions.PapayaIllegalAccessException;
import io.adenium.utils.ByteArray;

import java.util.Stack;

public class ReadOnlyHandler extends PapayaHandler {
    public ReadOnlyHandler(PapayaObject papayaObject) {
        super(papayaObject);
    }

    @Override
    public void setMember(ByteArray memberId, PapayaHandler member, Stack<io.adenium.papaya.compiler.Struct> stackTrace) throws PapayaIllegalAccessException {
        throw new PapayaIllegalAccessException();
    }

    @Override
    public PapayaHandler getMember(ByteArray memberId, Stack<Struct> stackTrace) throws PapayaIllegalAccessException {
        return new ReadOnlyHandler(getPapayaObject().getMember(memberId, stackTrace).getPapayaObject());
    }

    @Override
    public void call(Scope scope) throws PapayaException {
        getPapayaObject().call(scope);
    }

    @Override
    public PapayaHandler getAtIndex(int index) throws PapayaException {
        return new ReadOnlyHandler(getPapayaObject().asContainer().getAtIndex(index));
    }

    @Override
    public void setAtIndex(int index, PapayaHandler handler) throws PapayaException {
        throw new PapayaIllegalAccessException();
    }

    @Override
    public void append(PapayaHandler handler) throws PapayaException {
        throw new PapayaIllegalAccessException();
    }

    @Override
    public AccessModifier getModifier() {
        return AccessModifier.ReadOnly;
    }
}
