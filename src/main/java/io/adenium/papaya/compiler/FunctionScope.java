package io.adenium.papaya.compiler;

import io.adenium.exceptions.PapayaException;
import io.adenium.papaya.archive.ArchivedMember;
import io.adenium.papaya.archive.ArchivedMethod;
import io.adenium.papaya.archive.ArchivedStructureI;
import io.adenium.papaya.parser.Node;

import java.util.HashMap;
import java.util.Map;

public class FunctionScope {
    private final ArchivedMethod        method;
    private final ArchivedStructureI    parent;
    private int                         stack;
    private final Map<String, Integer>  tracker;
    private final Map<String, String[]> typeTracker;
    private final CompilationScope      compilationScope;
    private final ProgramWriter         writer;
    private String                      topOfStack[];

    public FunctionScope(ArchivedMethod method, ArchivedStructureI parent, CompilationScope scope) {
        this.method = method;
        this.parent = parent;
        this.stack  = 0;
        tracker = new HashMap<>();
        typeTracker = new HashMap<>();
        compilationScope = scope;
        writer = new ProgramWriter(scope.getOpcodeRegister());
        this.topOfStack = new String[0];
    }

    public void declare(ArchivedMember member) throws PapayaException {
        if (tracker.containsKey(member.getName())) {
            throw new PapayaException("redeclaration of symbol '" + member.getName() + "' at " + member.getLineInfo());
        }

        declare(member.getName(), member.getTypePath(), member.getLineInfo());
    }

    public void declare(String name, String[] type, LineInfo lineInfo) throws PapayaException {
        if (tracker.containsKey(name)) {
            throw new PapayaException("redeclaration of symbol '" + name + "' at " + lineInfo);
        }

        tracker.put(name, stack ++);
        typeTracker.put(name, type);
        topOfStack = type;
    }

    public boolean isTopOfStack(String name, LineInfo info) throws PapayaException {
        if (!tracker.containsKey(name)) {
            throw new PapayaException("reference to undeclared symbol '" + name + "' at " + info);
        }

        return tracker.get(name).equals(stack - 1);
    }

    public ArchivedStructureI getParent() {
        return parent;
    }

    public Map<String, Integer> getTracker() {
        return tracker;
    }

    public CompilationScope getCompilationScope() {
        return compilationScope;
    }

    public ProgramWriter getWriter() {
        return writer;
    }

    public void traverse(Node node, FunctionScope scope, CompilationScope compilationScope) throws PapayaException {
        if (compilationScope.getTraverserMap().containsKey(node.getTokenRule())) {
            compilationScope.getTraverserMap().get(node.getTokenRule()).onEnter(node, scope, compilationScope);
        } else if (node.getChildren().size() > 0) {
            for (Node child : node) {
                traverse(child, scope, compilationScope);
            }
        } else {
            throw new PapayaException("traverse-err: reached unknown rule '" + node.getTokenRule() + "' at " + node.getLineInfo());
        }
    }

    public FunctionScope push() {
        return new FunctionScope(method, parent, compilationScope);
    }

    public boolean contains(String ident) {
        return tracker.containsKey(ident) || (parent.containsMember(ident));
    }

    public void makeTop(String ident, LineInfo info) throws PapayaException {
        if (tracker.containsKey(ident)) {
            int distance = stack - tracker.get(ident);

            if (distance <= 16) {
                writer.write("dup" + distance);
            } else {
                writer.writeDup(distance);
            }

            topOfStack = typeTracker.get(ident);
        } else if (parent.containsMember(ident)) {
            ArchivedMember test = parent.getMember(ident);
            if (test != null) {
                ArchivedMember member = test;
                if (method.isStatic() && !member.isStatic()) {
                    throw new PapayaException("reference to '" + ident + "' from a static function at " + info);
                }

                if (member.isStatic()) {
                    writer.write("loadstatic", getCompilationScope().getMember(getCompilationScope().getTypeName(getCompilationScope().getPath()), ident).getArray());
                } else {
                    writer.write("load", getCompilationScope().getMember(getCompilationScope().getTypeName(getCompilationScope().getPath()), ident).getArray());
                }

                topOfStack = member.getTypePath();
            } else {
                ArchivedMethod member = parent.getMethod(ident);

                if (method.isStatic() && !member.isStatic()) {
                    throw new PapayaException("reference to '" + ident + "' from a static function at " + info);
                }

                if (member.isStatic()) {
                    writer.write("loadstatic", getCompilationScope().getMember(getCompilationScope().getTypeName(getCompilationScope().getPath()), ident).getArray());
                } else {
                    writer.write("load", getCompilationScope().getMember(getCompilationScope().getTypeName(getCompilationScope().getPath()), ident).getArray());
                }

                topOfStack = new String[] {"fun**"};
            }
        } else {
            throw new PapayaException("reference to undeclared symbol '" + ident + "' at " + info);
        }
    }

    public ArchivedMethod getMethod() {
        return method;
    }

    public void memberAccess(String ident, LineInfo lineInfo) throws PapayaException {
        if (topOfStack.length > 0) {
            ArchivedStructureI parent = compilationScope.getArchive().getStructure(topOfStack, lineInfo);

            if (parent.containsMember(ident)) {
                ArchivedMember test = parent.getMember(ident);
                if (test != null) {
                    ArchivedMember member = test;
                    if (member.isStatic()) {
                        writer.write("pop");
                        writer.write("loadstatic", getCompilationScope().getMember(getCompilationScope().getTypeName(topOfStack), ident).getArray());
                    } else {
                        writer.write("load", getCompilationScope().getMember(getCompilationScope().getTypeName(topOfStack), ident).getArray());
                    }

                    topOfStack = member.getTypePath();
                } else {
                    ArchivedMethod member = parent.getMethod(ident);

                    if (method.isStatic() && !member.isStatic()) {
                        throw new PapayaException("reference to '" + ident + "' from a static function at " + lineInfo);
                    }

                    if (member.isStatic()) {
                        writer.write("pop");
                        writer.write("loadstatic", getCompilationScope().getMember(getCompilationScope().getTypeName(topOfStack), ident).getArray());
                    } else {
                        writer.write("load", getCompilationScope().getMember(getCompilationScope().getTypeName(topOfStack), ident).getArray());
                    }

                    topOfStack = new String[] {"fun**"};
                }
            } else {
                throw new PapayaException("reference to undeclared symbol '" + ident + "' at " + lineInfo);
            }

            return;
        }

        throw new PapayaException("invalid type (top of stack).");
    }

    public int getStack() {
        return stack;
    }
}
