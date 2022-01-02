package io.adenium.papaya.archive;

import io.adenium.papaya.compiler.*;
import io.adenium.exceptions.PapayaException;

import java.util.List;

public class PapayaArchive implements ArchivedStructureI {
    private final ArchivedModule globalModule;

    public PapayaArchive() {
        globalModule = new ArchivedModule("global");
    }

    public void declare(String name, ArchivedMember field) throws PapayaException {
        throw new PapayaException("cannot declare fields in global module, at " + field.getLineInfo());
    }

    public void declare(String name, ArchivedMethod function) throws PapayaException {
        throw new PapayaException("cannot declare functions in global module, at " + function.getLineInfo());
    }

    public void declare(String name, ArchivedModule module) throws PapayaException {
        globalModule.declare(name, module);
    }

    public void declare(String name, ArchivedStruct struct) throws PapayaException {
        globalModule.declare(name, struct);
    }

    @Override
    public void declare(String name, ArchivedStructureI structure) throws PapayaException {
        globalModule.declare(name, structure);
    }

    public boolean containsName(String name) {
        return globalModule.containsName(name);
    }

    public boolean containsPath(String path[]) {
        ArchivedStructureI structure = globalModule;

        for (String id : path) {
            structure = structure.getModuleOrStruct(id);
            if (structure == null) return false;
        }

        return true;
    }

    public boolean containsMember(ArchivedType name) {
        ArchivedStructureI structure = globalModule;

        for (String id : name.getCanonicalName()) {
            structure = structure.getModuleOrStruct(id);
            if (structure == null) return false;
        }

        return true;
    }

    @Override
    public LineInfo getLineInfo() {
        return globalModule.getLineInfo();
    }

    @Override
    public ArchivedStructureI getModuleOrStruct(String name) {
        return globalModule.getModuleOrStruct(name);
    }

    @Override
    public boolean containsMember(String ident) {
        return globalModule.containsMember(ident);
    }

    @Override
    public ArchivedMember getMember(String ident) {
        return globalModule.getMember(ident);
    }

    @Override
    public ArchivedMethod getMethod(String ident) {
        return globalModule.getMethod(ident);
    }

    @Override
    public StructureType getStructureType() {
        return globalModule.getStructureType();
    }

    public String formattedString() {
        return globalModule.formattedString();
    }

    public List<Struct> getStructures() {
        return globalModule.getStructures();
    }

    public ArchivedMethod getEntryPoint() {
        return globalModule.getEntryPoint();
    }

    public PapayaApplication compile(String compilerArguments, CompilationScope compilationScope) throws PapayaException {
        PapayaApplication application = new PapayaApplication(1);
        globalModule.compile(application, compilationScope);

        return application;
    }

    public PapayaArchive insert(PapayaArchive libraries) throws PapayaException {
        globalModule.insert(libraries.globalModule);
        return this;
    }
}
