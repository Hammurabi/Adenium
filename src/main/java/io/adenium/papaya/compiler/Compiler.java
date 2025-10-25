package io.adenium.papaya.compiler;

import io.adenium.core.Context;
import io.adenium.exceptions.PapayaException;
import io.adenium.exceptions.AdeniumException;
import io.adenium.papaya.archive.ArchivedMember;
import io.adenium.papaya.archive.ArchivedStruct;
import io.adenium.papaya.archive.PapayaArchive;
import io.adenium.papaya.parser.Node;
import io.adenium.utils.ByteArray;

import java.io.IOException;
import java.util.Map;

public abstract class Compiler {
    private Map<String, ByteArray> typeNames;

    public static Compiler getFor(String language) throws AdeniumException, IOException, PapayaException {
        if (language.toLowerCase().equals("papaya")) {
            language = "papaya+v0.01a";
        }

        if (language.toLowerCase().equals("papaya+v0.01a")) {
            return new PapayaCompiler(Context.getInstance().getOpcodeRegister());
        }

        throw new AdeniumException("compiler for language '" + language + "' could not be found.");
    }

    public abstract PapayaArchive createArchive(String text, String compilerArguments) throws PapayaException, AdeniumException, IOException;
    public abstract PapayaApplication compile(PapayaArchive archive, PapayaArchive libraries, String compilerArguments) throws PapayaException, AdeniumException, IOException;

    public ByteArray uniqueTypename(String type) {
        return typeNames.get(type);
    }

    public abstract Expression compile(ArchivedStruct parent, ArchivedMember archivedMember, Node expression) throws PapayaException;
}
