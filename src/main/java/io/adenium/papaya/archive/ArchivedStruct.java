package io.adenium.papaya.archive;

import io.adenium.papaya.compiler.*;
import io.adenium.exceptions.PapayaException;
import io.adenium.utils.ByteArray;
import io.adenium.utils.Pair;

import java.util.HashMap;
import java.util.Map;

public class ArchivedStruct implements ArchivedStructureI {
    private final String                        name;
    private final StructureType type;
    private final LineInfo lineInfo;
    private final Map<String, ArchivedMember>   memberMap;
    private final Map<String, ArchivedMethod>   methodMap;

    public ArchivedStruct(String name, StructureType type, LineInfo lineInfo) {
        this.name = name;
        this.type = type;
        this.lineInfo = lineInfo;
        this.memberMap = new HashMap<>();
        this.methodMap = new HashMap<>();
    }

    public void declare(String name, ArchivedMember member) throws PapayaException {
        if (memberMap.containsKey(name) || methodMap.containsKey(name)) {
            throw new PapayaException("redeclaration of member '" + name + "' {" + member.getLineInfo() + "}.");
        }

        if (type == StructureType.StructType) {
            if (member.getAccessModifier() == AccessModifier.None) {
                member.setAccessModifier(AccessModifier.PublicAccess);
            } else if (member.getAccessModifier() != AccessModifier.PublicAccess && member.getAccessModifier() != AccessModifier.ReadOnly) {
                throw new PapayaException("members of 'struct' can only be public. '" + name + "' {" + member.getLineInfo() + "}.");
            }
        } else {
            if (member.getAccessModifier() == AccessModifier.None) {
                member.setAccessModifier(AccessModifier.PrivateAccess);
            }
        }

        memberMap.put(name, member);
    }

    public void declare(String name, ArchivedMethod member) throws PapayaException {
        if (memberMap.containsKey(name) || methodMap.containsKey(name)) {
            throw new PapayaException("redeclaration of member '" + name + "' {" + member.getLineInfo() + "}.");
        }

        methodMap.put(name, member);
    }

    @Override
    public void declare(String name, ArchivedModule module) throws PapayaException {
        throw new PapayaException("inner classes are not supported.");
    }

    @Override
    public void declare(String name, ArchivedStruct struct) throws PapayaException {
        throw new PapayaException("inner classes are not supported.");
    }

    @Override
    public void declare(String name, ArchivedStructureI structure) throws PapayaException {
        throw new PapayaException("inner classes are not supported.");
    }

    @Override
    public boolean containsName(String name) {
        return memberMap.containsKey(name) || methodMap.containsKey(name);
    }

    public String getName() {
        return name;
    }

    public StructureType getType() {
        return type;
    }

    public LineInfo getLineInfo() {
        return lineInfo;
    }

    @Override
    public ArchivedStructureI getModuleOrStruct(String name) {
        return null;
    }

    @Override
    public boolean containsMember(String ident) {
        return memberMap.containsKey(ident) || methodMap.containsKey(ident);
    }

    @Override
    public ArchivedMember getMember(String ident) {
        return memberMap.get(ident);
    }

    @Override
    public ArchivedMethod getMethod(String ident) {
        return methodMap.get(ident);
    }

    @Override
    public StructureType getStructureType() {
        return getType();
    }

    public ByteArray uniqueName(String name, ArchivedStruct parent) {
        return null;
    }

    public String formattedString(int i) {
        StringBuilder builder = new StringBuilder();
        StringBuilder tabs = new StringBuilder();
        for (int t = 0; t < i; t ++) {
            tabs.append("\t");
        }

        builder.append(tabs).append(type).append(" ").append(name).append("\n");

        for (String member : memberMap.keySet()) {
            builder.append(tabs).append(memberMap.get(member).formattedString(i + 1));
        }

        for (String method : methodMap.keySet()) {
            builder.append(tabs).append(methodMap.get(method).formattedString(i + 1));
        }

        return builder.toString();
    }

    public String formattedString() {
        return formattedString(0);
    }

    public void compile(PapayaApplication application, CompilationScope compilationScope) throws PapayaException {
        compilationScope.enterClass(this, name);
        Struct structure = new Struct(name, type, lineInfo);

        for (ArchivedMember member : memberMap.values()) {
            member.compile(application, compilationScope);
        }

        for (ArchivedMethod method : methodMap.values()) {
            Pair<Member, String> pair = method.compile(compilationScope);
            System.out.println(pair.getSecond());
        }
    }

    public void insert(ArchivedStruct other) throws PapayaException {
        for (String member : other.memberMap.keySet()) {
            declare(member, other.memberMap.get(member));
        }

        for (String method : other.methodMap.keySet()) {
            declare(method, other.methodMap.get(method));
        }
    }
}
