package io.adenium.papaya.runtime;

import io.adenium.papaya.compiler.AccessModifier;
import io.adenium.papaya.compiler.Struct;
import io.adenium.exceptions.PapayaException;
import io.adenium.exceptions.PapayaIllegalAccessException;
import io.adenium.utils.ByteArray;

import java.util.Stack;

public class DefaultHandler extends PapayaHandler {
    public DefaultHandler(PapayaObject object) {
        super(object);
    }

    @Override
    public void setMember(ByteArray memberId, PapayaHandler member, Stack<io.adenium.papaya.compiler.Struct> stackTrace) throws PapayaIllegalAccessException {
        getPapayaObject().setMember(memberId, member, stackTrace);
    }

    @Override
    public PapayaHandler getMember(ByteArray memberId, Stack<Struct> stackTrace) throws PapayaIllegalAccessException {
        return getPapayaObject().getMember(memberId, stackTrace);
    }

    @Override
    public void call(Scope scope) throws PapayaException {
        getPapayaObject().call(scope);
    }

    @Override
    public PapayaHandler getAtIndex(int index) throws PapayaException {
        return new DefaultHandler(getPapayaObject().asContainer().getAtIndex(index));
    }

    @Override
    public void setAtIndex(int index, PapayaHandler handler) throws PapayaException {
        if (handler.isReadOnly()) {
            throw new PapayaIllegalAccessException();
        }

        getPapayaObject().asContainer().setAtIndex(index, handler);
    }

    @Override
    public void append(PapayaHandler handler) throws PapayaException {
        getPapayaObject().asContainer().append(handler);
    }

    @Override
    public AccessModifier getModifier() {
        return AccessModifier.None;
    }
}
