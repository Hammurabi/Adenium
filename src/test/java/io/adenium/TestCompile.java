package io.adenium;

import io.adenium.core.ResourceManager;
import io.adenium.crypto.CryptoLib;
import io.adenium.exceptions.PapayaException;
import io.adenium.exceptions.WolkenException;
import io.adenium.papaya.archive.PapayaArchive;
import io.adenium.papaya.compiler.PapayaApplication;
import io.adenium.papaya.compiler.PapayaCompiler;
import org.wolkenproject.papaya.compiler.*;
import io.adenium.papaya.compiler.Compiler;
import io.adenium.papaya.runtime.OpcodeRegister;

import java.io.IOException;

public class TestCompile {
    public void testCompile() throws WolkenException, IOException, PapayaException {
        CryptoLib.getInstance();
        String program = ResourceManager.getString("/papaya/contract.pya");
        String librariesProgram = ResourceManager.getString("/papaya/libraries.pya");
        OpcodeRegister opcodeRegister = new OpcodeRegister();
        OpcodeRegister.register(opcodeRegister);
        Compiler compiler = new PapayaCompiler(opcodeRegister);
        PapayaArchive archive = compiler.createArchive(program, "-identifiers sequential");
        PapayaArchive libraries = compiler.createArchive(librariesProgram, "-identifiers sequential");
        PapayaApplication application = compiler.compile(archive, libraries, "-identifiers sequential");

        System.out.println(application.toString());
    }

    public static void main(String args[]) throws WolkenException, IOException, PapayaException {
        new TestCompile().testCompile();
    }
}
