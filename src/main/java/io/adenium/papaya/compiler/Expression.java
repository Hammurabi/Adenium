package io.adenium.papaya.compiler;

import io.adenium.encoders.Base16;
import io.adenium.utils.Utils;

public class Expression {
    private byte[]    opcodes;
    private LineInfo  lineInfo;

    public Expression(LineInfo lineInfo) {
        this(new byte[0], lineInfo);
    }

    public Expression(byte[] opcodes, LineInfo lineInfo) {
        this.opcodes = opcodes;
        this.lineInfo= lineInfo;
    }

    public void add(int opcode) {
        int length = opcodes.length;
        byte[] n = new byte[length + 1];
        System.arraycopy(opcodes, 0, n, 0, length);
        n[length] = (byte) opcode;
        opcodes = n;
    }

    public void add(byte[] ops) {
        opcodes = Utils.concatenate(opcodes, ops);
    }

    public byte[] getOpcodes() {
        return opcodes;
    }

    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public String toString(int indentations) {
        StringBuilder builder = new StringBuilder();
        StringBuilder indentation = new StringBuilder();
        for (int i = 0; i < indentations; i ++) {
            indentation.append("\t");
        }

        builder.append(indentation).append("\t");
        int off = 1;

        for (int i = 0; i < opcodes.length; i ++) {
            builder.append("0x").append(Base16.encode(new byte[] { opcodes[i] })).append(" ");
            if (i != opcodes.length - 1 && off ++ % 8 == 0) {
                builder.append("\n").append(indentation).append("\t");
            }
        }

        builder.append("\n").append(indentation).append("at line (").append(lineInfo.getLine()).append(")\n");

        return builder.toString();
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public void setLineInfo(LineInfo lineInfo) {
        this.lineInfo = lineInfo;
    }
}
