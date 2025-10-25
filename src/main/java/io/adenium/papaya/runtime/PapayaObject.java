package io.adenium.papaya.runtime;

import io.adenium.papaya.compiler.Member;
import io.adenium.papaya.compiler.Struct;
import io.adenium.exceptions.PapayaException;
import io.adenium.exceptions.PapayaIllegalAccessException;
import io.adenium.utils.ByteArray;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/*
    PapayaObjects do not expose the underlying memory structure
    for safety reasons, therefore, children of objects are accessible
    by <32Bit> identifiers, it also allows implementation specific access
    to object children.
 */
public class PapayaObject {
    /*
        A pointer to a structure object which holds information
        about the object, number of fields and functions, etc.
     */
    private io.adenium.papaya.compiler.Struct structure;
    /*
        A callable object.
     */
    private PapayaCallable callable;
    /*
        Using a map structure allows us to call members by name
        ie: object.member = .. ===> object.map(hash('member')) = ..
        this allows us to DYNAMICALLY add members to an object.
     */
    private Map<ByteArray, PapayaObject> members;

    public PapayaObject() {
        this(PapayaCallable.Default);
    }

    public PapayaObject(PapayaCallable callable) {
        this.callable = callable;
        this.members = new HashMap<>();
    }

    public void setMember(ByteArray memberId, PapayaHandler member, Stack<io.adenium.papaya.compiler.Struct> stackTrace) throws PapayaIllegalAccessException {
        io.adenium.papaya.compiler.Member classMember = structure.getMember(memberId);
        structure.checkWriteAccess(classMember, stackTrace);
        members.put(memberId, member.getPapayaObject());
    }

    public PapayaHandler getMember(ByteArray memberId, Stack<io.adenium.papaya.compiler.Struct> stackTrace) throws PapayaIllegalAccessException {
        Member classMember = structure.getMember(memberId);
        return structure.checkMemberAccess(classMember, members.get(memberId), stackTrace);
    }

    public void call(Scope scope) throws PapayaException {
        callable.call(scope);
    }

    public boolean asBool() {
        return false;
    }

    /*
        Returns an unsigned integer of size <1-256> bits
     */
    public BigInteger asInt() {
        return BigInteger.ZERO;
    }

    /*
        Returns a signed integer of size <1-256> bits
     */
    public BigInteger asSignedInt() {
        return BigInteger.ZERO;
    }

    public PapayaContainer asContainer() throws PapayaException {
        throw new PapayaException("'"+structure.getName()+"' is not a container.");
    }

    public Struct getStructure() {
        return structure;
    }
}
