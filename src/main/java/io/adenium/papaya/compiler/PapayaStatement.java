package io.adenium.papaya.compiler;

import io.adenium.encoders.Base16;

import java.util.ArrayList;
import java.util.List;

public class PapayaStatement {
    private final static StatementCompiler DefaultCompiler = scope -> {};
    private final List<PapayaStatement> children;
    private final LineInfo              lineInfo;
    private final StatementCompiler     compiler;
    private final byte                  opcodes[];

    public PapayaStatement(LineInfo lineInfo, byte opcodes[]) {
        this(DefaultCompiler, lineInfo, opcodes);
    }

    public PapayaStatement(StatementCompiler compiler, LineInfo lineInfo, byte opcodes[]) {
        this.children = new ArrayList<>();
        this.lineInfo = lineInfo;
        this.compiler = compiler;
        this.opcodes  = opcodes;
    }

    public void compile(CompilationScope scope) {
        compiler.compile(scope);
        for (PapayaStatement statement : children) {
            statement.compile(scope);
        }
    }

    public LineInfo getLineInfo() {
        if (lineInfo.getLine() == -1) {
            if (!children.isEmpty()) {
                return children.get(0).getLineInfo();
            }
        }

        return lineInfo;
    }

    public PapayaStatement addChild(PapayaStatement papayaStatement) {
        children.add(papayaStatement);
        return this;
    }

    public String toString(int indentations) {
        StringBuilder builder = new StringBuilder();
        StringBuilder indentation = new StringBuilder();
        for (int i = 0; i < indentations; i ++) {
            indentation.append("\t");
        }

        builder.append(indentations).append("\t");

        for (byte opcode : opcodes) {
            builder.append("0x").append(Base16.encode(new byte[] { opcode })).append(" ");
        }

        builder.append(indentation).append("at line (").append(lineInfo.getLine()).append(")");

        return builder.toString();
    }
}
