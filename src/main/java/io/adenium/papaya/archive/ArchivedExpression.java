package io.adenium.papaya.archive;

import io.adenium.papaya.compiler.Compiler;
import io.adenium.papaya.compiler.Expression;
import io.adenium.papaya.compiler.LineInfo;

public class ArchivedExpression {
    private final LineInfo lineInfo;

    public ArchivedExpression(LineInfo lineInfo) {
        this.lineInfo = lineInfo;
    }

    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public Expression compile(ArchivedStruct parent, Compiler compiler) {
        return null;
    }
}
