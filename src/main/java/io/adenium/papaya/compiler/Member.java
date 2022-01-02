package io.adenium.papaya.compiler;

import io.adenium.encoders.Base16;
import io.adenium.utils.ByteArray;

import java.math.BigInteger;

public class Member {
    private final AccessModifier        accessModifier;
    private final boolean               isStatic;
    private final ByteArray             identifier;
    private final ByteArray             enforcedType;
    private final LineInfo              lineInfo;
    private final byte                  expression[];

    public Member(AccessModifier accessModifier, ByteArray identifier, ByteArray enforcedType, LineInfo lineInfo) {
        this(accessModifier, false, identifier, enforcedType, lineInfo);
    }

    public Member(AccessModifier accessModifier, boolean isStatic, ByteArray identifier, ByteArray enforcedType, LineInfo lineInfo) {
        this(accessModifier, false, identifier, enforcedType, lineInfo, new byte[0]);
    }

    public Member(AccessModifier accessModifier, boolean isStatic, ByteArray identifier, ByteArray enforcedType, LineInfo lineInfo, byte opcodes[]) {
        this.accessModifier = accessModifier;
        this.isStatic = isStatic;
        this.enforcedType = enforcedType;
        this.identifier = identifier;
        this.lineInfo = lineInfo;
        this.expression = opcodes;
    }

    public AccessModifier getAccessModifier() {
        return accessModifier;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public ByteArray getIdentifier() {
        return identifier;
    }

    public BigInteger getIdentifierInt() {
        return new BigInteger(1, getIdentifier().getArray());
    }

    public byte[] getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\tmember (").append(new String(getIdentifier().getArray())).append(")");
        if (enforcedType != null && !enforcedType.isNull()) builder.append(" ").append(new String(enforcedType.getArray()));
        builder.append(" access (").append(getAccessModifier()).append(")\n");
        if (expression != null) {
            builder.append(toOpcodeString(2));
        }

        return builder.toString();
    }



    public String toOpcodeString(int indentations) {
        StringBuilder builder = new StringBuilder();
        StringBuilder indentation = new StringBuilder();
        for (int i = 0; i < indentations; i ++) {
            indentation.append("\t");
        }

        builder.append(indentation).append("\t");
        int off = 1;

        for (int i = 0; i < expression.length; i ++) {
            builder.append("0x").append(Base16.encode(new byte[] { expression[i] })).append(" ");
            if (i != expression.length - 1 && off ++ % 8 == 0) {
                builder.append("\n").append(indentation).append("\t");
            }
        }

        builder.append("\n").append(indentation).append("at line (").append(lineInfo.getLine()).append(")\n");

        return builder.toString();
    }

    public LineInfo getLineInfo() {
        return lineInfo;
    }
}
