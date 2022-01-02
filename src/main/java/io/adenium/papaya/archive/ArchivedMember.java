package io.adenium.papaya.archive;

import io.adenium.papaya.compiler.*;
import io.adenium.exceptions.PapayaException;
import io.adenium.papaya.parser.Node;

import java.util.Arrays;

public class ArchivedMember {
    private final String                name;
    private final String                type[];
    private AccessModifier accessModifier;
    private final boolean               isStatic;
    private final LineInfo lineInfo;
    private final Node                  expression;

    public ArchivedMember(String name, String typeName[], AccessModifier accessModifier, boolean isStatic, LineInfo lineInfo, Node expression) {
        this.name = name;
        this.type = typeName;
        this.accessModifier = accessModifier;
        this.isStatic = isStatic;
        this.lineInfo = lineInfo;
        this.expression = expression;
    }

    public Member compile(PapayaApplication parent, CompilationScope compiler) throws PapayaException {
//        Expression expression = null;
//
//        if (expression != null) {
//            expression = compiler.compile(parent, this, this.expression);
//        }

//        return new PapayaMember(accessModifier, isStatic, parent.uniqueName(name, parent), compiler.uniqueTypename(type), expression, lineInfo);
        return null;
    }

    public String getName() {
        return name;
    }

    public String[] getTypePath() {
        return type;
    }

    public String getTypeName() {
        return type[type.length - 1];
    }

    public AccessModifier getAccessModifier() {
        return accessModifier;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public LineInfo getLineInfo() {
        return lineInfo;
    }

    public Node getExpression() {
        return expression;
    }

    public String formattedString(int i) {
        StringBuilder builder = new StringBuilder();
        StringBuilder tabs = new StringBuilder();
        for (int t = 0; t < i; t ++) {
            tabs.append("\t");
        }

        StringBuilder type = new StringBuilder();
        for (int p = 0; p < this.type.length; p ++) {
            type.append(this.type[p]);
            if (p < this.type.length - 1) {
                type.append(".");
            }
        }

        builder.append(tabs).append(type).append(" ").append(name).append(" ").append(accessModifier).append(" ").append(isStatic?"static":"").append(" ").append(lineInfo).append("\n");
        if (expression != null) {
            expression.toString(i + 2, builder);
        }

        return builder.toString();
    }

    public String formattedString() {
        return formattedString(0);
    }

    public void setAccessModifier(AccessModifier accessModifier) {
        this.accessModifier = accessModifier;
    }

    @Override
    public String toString() {
        return "ArchivedMember{" +
                "name='" + name + '\'' +
                ", type=" + Arrays.toString(type) +
                ", accessModifier=" + accessModifier +
                ", isStatic=" + isStatic +
                ", lineInfo=" + lineInfo +
                ", expression=" + expression +
                '}';
    }
}
