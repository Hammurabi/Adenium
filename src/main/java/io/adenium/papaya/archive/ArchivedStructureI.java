package io.adenium.papaya.archive;

import io.adenium.papaya.compiler.LineInfo;
import io.adenium.papaya.compiler.StructureType;
import io.adenium.exceptions.PapayaException;

import java.util.Arrays;

public interface ArchivedStructureI {
    public void declare(String name, ArchivedMember field) throws PapayaException;
    public void declare(String name, ArchivedMethod function) throws PapayaException;
    public void declare(String name, ArchivedModule module) throws PapayaException;
    public void declare(String name, ArchivedStruct struct) throws PapayaException;
    public void declare(String name, ArchivedStructureI structure) throws PapayaException;
    public boolean containsName(String name);

    LineInfo getLineInfo();
    ArchivedStructureI getModuleOrStruct(String name);

    default ArchivedStructureI getStructure(String path[], LineInfo lineInfo) throws PapayaException {
        return getStructure(path, lineInfo, 0);
    }

    default ArchivedStructureI getStructure(String path[], LineInfo lineInfo, int i) throws PapayaException {
        ArchivedStructureI structure = getModuleOrStruct(path[i]);
        if (structure == null) {
            throw new PapayaException("reference to structure '" + path[i] + "' from path '"+ Arrays.toString(path) +"' not found at " + lineInfo);
        }

        if (i + 1 < path.length - 1) {
            return structure.getStructure(path, lineInfo, i + 1);
        }

        return structure;
    }

    boolean containsMember(String ident);
    ArchivedMember getMember(String ident);
    ArchivedMethod getMethod(String ident);

    StructureType getStructureType();
}
