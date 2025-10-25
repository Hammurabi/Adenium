package io.adenium.papaya.runtime;

import io.adenium.papaya.compiler.AccessModifier;
import io.adenium.papaya.compiler.Member;
import io.adenium.papaya.compiler.StructureType;
import io.adenium.exceptions.PapayaIllegalAccessException;
import io.adenium.utils.ByteArray;

import java.util.Map;
import java.util.Objects;
import java.util.Stack;

public class Struct {
    private final ByteArray                 identifier;
    private final StructureType type;
    private final Map<ByteArray, io.adenium.papaya.runtime.Member>    memberMap;

    public Struct(ByteArray identifier, StructureType type, Map<ByteArray, io.adenium.papaya.runtime.Member> memberMap) {
        this.identifier = identifier;
        this.type = type;
        this.memberMap = memberMap;
    }

    public ByteArray getIdentifier() {
        return identifier;
    }

    public StructureType getType() {
        return type;
    }

    public boolean containsMember(ByteArray member) {
        return memberMap.containsKey(member);
    }

    public void checkWriteAccess(io.adenium.papaya.compiler.Member member, Stack<Struct> stackTrace) throws PapayaIllegalAccessException {
        AccessModifier modifier = member.getAccessModifier();
        if (modifier == AccessModifier.ReadOnly) {
            throw new PapayaIllegalAccessException();
        }

        if (!stackTrace.peek().getIdentifier().equals(getIdentifier())) {
            switch (modifier) {
                case PrivateAccess:
                    throw new PapayaIllegalAccessException();
                case ProtectedAccess:
                    if (!stackTrace.peek().isChildOf(this)) {
                        throw new PapayaIllegalAccessException();
                    }
                    break;
            }
        }
    }

    public PapayaHandler checkMemberAccess(io.adenium.papaya.compiler.Member classMember, PapayaObject member, Stack<Struct> stackTrace) throws PapayaIllegalAccessException {
        AccessModifier modifier = classMember.getAccessModifier();
        // if it is read only, then we return a wrapper object.
        if (modifier == AccessModifier.PublicAccess) {
            return PapayaHandler.doNothingHandler(member);
        } else if (modifier == AccessModifier.ReadOnly) {
            return PapayaHandler.readOnlyHandler(member);
        }

        if (!stackTrace.peek().getIdentifier().equals(getIdentifier())) {
            switch (modifier) {
                case PrivateAccess:
                    throw new PapayaIllegalAccessException();
                case ProtectedAccess:
                    if (!stackTrace.peek().isChildOf(this)) {
                        throw new PapayaIllegalAccessException();
                    }
                    break;
            }
        }

        return PapayaHandler.doNothingHandler(member);
    }

    public void checkReadAccess(Member member, Stack<Struct> stackTrace) throws PapayaIllegalAccessException {
        AccessModifier modifier = member.getAccessModifier();
        if (modifier == AccessModifier.ReadOnly || modifier == AccessModifier.PublicAccess) {
            return;
        }

        if (!stackTrace.peek().getIdentifier().equals(getIdentifier())) {
            switch (modifier) {
                case PrivateAccess:
                    throw new PapayaIllegalAccessException();
                case ProtectedAccess:
                    if (!stackTrace.peek().isChildOf(this)) {
                        throw new PapayaIllegalAccessException();
                    }
                    break;
            }
        }
    }

    public boolean isChildOf(Struct parent) {
        if (parent != null) {
            if (parent.getIdentifier().equals(getIdentifier())) {
                return true;
            }

            return parent.isChildOf(parent);
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Struct struct = (Struct) o;
        return Objects.equals(identifier, struct.identifier) && type == struct.type && Objects.equals(memberMap, struct.memberMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, type, memberMap);
    }
}
