package io.adenium.papaya.compiler;

import io.adenium.exceptions.PapayaException;
import io.adenium.papaya.archive.ArchivedMethod;
import io.adenium.papaya.archive.ArchivedStructureI;
import io.adenium.papaya.archive.PapayaArchive;
import io.adenium.papaya.runtime.OpcodeRegister;
import io.adenium.utils.ByteArray;
import io.adenium.utils.VarInt;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class CompilationScope {
    private final Map<String, ByteArray>    typenames;
    private final Stack<ArchivedStructureI> classTrace;
    private final Stack<FunctionScope>      functionTrace;
    private final Stack<String>             path;
    private final PapayaArchive             archive;
    private final OpcodeRegister            opcodeRegister;
    private final Map<String, Traverser>    traverserMap;
    private final Map<ByteArray, Map<String, ByteArray>> typeMembers;

    public CompilationScope(PapayaArchive archive, PapayaArchive libraries, OpcodeRegister opcodeRegister, Map<String, Traverser> traverserMap) throws PapayaException {
        classTrace = new Stack<>();
        functionTrace = new Stack<>();
        path = new Stack<>();
        this.archive = archive.insert(libraries);
        this.opcodeRegister = opcodeRegister;
        this.traverserMap = traverserMap;
        this.typenames = new HashMap<>();
        this.typeMembers = new HashMap<>();
        prebuild(libraries);
    }

    private void prebuild(PapayaArchive libraries) {
    }

    public void enterClass(ArchivedStructureI structure, String name) {
        path.push(name);
        classTrace.push(structure);
    }

    public FunctionScope enterFunction(ArchivedMethod method) {
        FunctionScope scope = null;
        functionTrace.push(scope = new FunctionScope(method, classTrace.peek(), this));

        return scope;
    }

    public void exitClass() {
        classTrace.pop();
    }

    public ProgramWriter exitFunction() {
        return functionTrace.pop().getWriter();
    }

    public OpcodeRegister getOpcodeRegister() {
        return opcodeRegister;
    }

    public PapayaArchive getArchive() {
        return archive;
    }

    public Map<String, Traverser> getTraverserMap() {
        return traverserMap;
    }

    public ByteArray getTypeName(String name[]) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length; i ++) {
            builder.append(name[i]);
            if (i < name.length - 1) {
                builder.append("::");
            }
        }

        return getTypeName(builder.toString());
    }

    public ByteArray getTypeName(String name) {
        if (typenames.containsKey(name)) {
            return typenames.get(name);
        }

        typenames.put(name, ByteArray.wrap(VarInt.writeCompactUInt32(typenames.size(), false)));
        return typenames.get(name);
    }

    public ByteArray getMember(ByteArray type, String memberName) {
        if (!typeMembers.containsKey(type)) {
            typeMembers.put(type, new HashMap<>());
        }

        if (typeMembers.get(type).containsKey(memberName)) {
            return typeMembers.get(type).get(memberName);
        }

        typeMembers.get(type).put(memberName, ByteArray.wrap(VarInt.writeCompactUInt32(typeMembers.get(type).size(), false)));
        return typeMembers.get(type).get(memberName);
    }

    public String[] getPath() {
        String[] path = new String[this.path.size()];
        return this.path.toArray(path);
    }
}
