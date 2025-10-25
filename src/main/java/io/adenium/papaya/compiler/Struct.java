package io.adenium.papaya.compiler;

import io.adenium.papaya.runtime.PapayaCallable;
import io.adenium.papaya.runtime.PapayaObject;
import io.adenium.exceptions.PapayaException;
import io.adenium.exceptions.PapayaIllegalAccessException;
import io.adenium.exceptions.AdeniumException;
import io.adenium.papaya.runtime.PapayaHandler;
import io.adenium.utils.ByteArray;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Struct {
    public static final int                     Alignment = 32;
    private final ByteArray                     identifier;
    private final StructureType                 structureType;
    private final Set<Member>                   members;
    private final Map<Integer, PapayaCallable>  operators;
    private final LineInfo                      lineInfo;
    private long                                structureLength;

    public Struct(String name, StructureType structureType, LineInfo lineInfo) {
        this.structureType  = structureType;
        this.members        = new LinkedHashSet<>();
        this.operators      = new HashMap<>();
        this.lineInfo       = lineInfo;
        this.identifier     = ByteArray.wrap(name.getBytes(StandardCharsets.UTF_8));
    }

    public PapayaCallable getOperator(int operator) throws PapayaException {
        if (operators.containsKey(operator)) {
            return operators.get(operator);
        }

        throw new PapayaException("operator '"+operator+"' is not overloaded for '"+getName()+"'.");
    }

    public void addField(String name, Member field) throws PapayaException {
        if (containsMember(name)) {
            throw new PapayaException("redeclaration of field '" + name + "' "+field.getLineInfo()+".");
        }

        members.add(field);
    }

    public void addFunction(String name, PapayaFunction function) throws PapayaException {
        if (containsMember(name)) {
            throw new PapayaException("redeclaration of function '" + name + "' "+function.getLineInfo()+".");
        }

        members.add(function);
    }

    public void addMember(String name, Member member) throws PapayaException {
        if (containsMember(name)) {
            throw new PapayaException("redeclaration of member '" + name + "'.");
        }

        members.add(member);
    }

    public boolean containsMember(String name) {
        for (Member member : members) {
            if (member.getIdentifier().equals(ByteArray.wrap(name.getBytes(StandardCharsets.UTF_8)))) {
                return true;
            }
        }

        return false;
    }

    public boolean containsMemberId(ByteArray identifier) {
        for (Member member : members) {
            if (member.getIdentifier().equals(identifier)) {
                return true;
            }
        }

        return false;
    }

    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public void compile(PapayaApplication application) throws AdeniumException {
        for (PapayaFunction function : getFunctions()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//            CompilationScope compilationScope = new CompilationScope(outputStream, application, node);
//            function.compile(compilationScope);
//            function.setByteCode(outputStream.toByteArray());
        }
    }

    public Set<Member> getFields() {
        Set<Member> functions = new LinkedHashSet<>();
        for (Member member : getMembers()) {
                functions.add(member);
        }

        return functions;
    }

    public Set<PapayaFunction> getFunctions() {
        Set<PapayaFunction> functions = new LinkedHashSet<>();
        for (Member member : getMembers()) {
            if (member instanceof PapayaFunction) {
                functions.add((PapayaFunction) member);
            }
        }

        return functions;
    }

    public void checkWriteAccess(Member member, Stack<Struct> stackTrace) throws PapayaIllegalAccessException {
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

    public PapayaHandler checkMemberAccess(Member classMember, PapayaObject member, Stack<Struct> stackTrace) throws PapayaIllegalAccessException {
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

    public Member getMember(ByteArray identifier) {
        for (Member member : members) {
            if (member.getIdentifier().equals(identifier)) {
                return member;
            }
        }

        return null;
    }

    public final ByteArray getIdentifier() {
        return identifier;
    }

    public StructureType getStructureType() {
        return structureType;
    }

    public String getName() {
        return new String(identifier.getArray());
    }

    public Set<Member> getMembers() {
        return members;
    }

    public BigInteger getIdentifierInt() {
        return new BigInteger(1, getIdentifier().getArray());
    }

    public static final class Operator {
        public static final int
                None = 0,

                Add = 1,
                Sub = 2,
                Mul = 3,
                Div = 4,
                Mod = 5,
                Pow = 6,

                And = 7,
                Or = 8,
                Xor = 9,

                UnsignedShift = 10,
                RightShift = 11,
                LeftShift = 12,

                Not = 13,
                Negate = 14,

                LessThan = 15,
                GreaterThan = 16,
                LessThanOrEqual = 17,
                GreaterThanOrEqual = 18,
                Equals = 19,
                NotEquals = 20;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getName()).append("\n");
        for (Member member : members) {
            builder.append(member.toString());
        }

        return builder.toString();
    }
}
