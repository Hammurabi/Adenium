package io.adenium.papaya.compiler;

import io.adenium.encoders.Base16;
import io.adenium.exceptions.PapayaException;
import io.adenium.exceptions.AdeniumException;
import io.adenium.papaya.runtime.OpcodeRegister;
import io.adenium.utils.VarInt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

public class ProgramWriter {
    private OpcodeRegister          opcodeRegister;
    private ByteArrayOutputStream   outputStream;
    private StringBuilder           builder;

    public ProgramWriter(OpcodeRegister register) {
        opcodeRegister = register;
        outputStream = new ByteArrayOutputStream();
        builder = new StringBuilder();
    }

    public void write(String opcode) {
        int opcodeValue = opcodeRegister.forName(opcode);
        builder.append("Op" + opcode.substring(0, 1).toUpperCase() + opcode.substring(1));
        builder.append("(").append("0x").append(Base16.encode(new byte[] { (byte) opcodeValue })).append(")\n");
    }

    public void write(String opcode, byte[] data) throws PapayaException {
        write(opcode);
        try {
            VarInt.writeCompactUInt32(data.length, false, outputStream);
            outputStream.write(data);
        } catch (IOException e) {
            throw new PapayaException(e);
        }
        builder.append("\t");
        builder.append(data.length);
        builder.append(" ");
        builder.append(Arrays.toString(data)).append("\n");
    }

    public void writeNInt32(long integer, boolean preserve) throws PapayaException {
        String opcode = "nconst";
        int opcodeValue = opcodeRegister.forName(opcode);
        builder.append("Op" + opcode.substring(0, 1).toUpperCase() + opcode.substring(1));
        builder.append("(").append("0x").append(Base16.encode(new byte[] { (byte) opcodeValue })).append(")\n");
        builder.append("\t").append(integer).append(" ").append(preserve).append("\n");
        try {
            VarInt.writeCompactUInt32(~integer, preserve, outputStream);
        } catch (IOException e) {
            throw new PapayaException(e);
        }
    }

    public void writeNInt64(long integer, boolean preserve) throws PapayaException {
        String opcode = "nconst";
        int opcodeValue = opcodeRegister.forName(opcode);
        builder.append("Op" + opcode.substring(0, 1).toUpperCase() + opcode.substring(1));
        builder.append("(").append("0x").append(Base16.encode(new byte[] { (byte) opcodeValue })).append(")\n");
        builder.append("\t").append(integer).append(" ").append(preserve).append("\n");
        try {
            VarInt.writeCompactUInt32(~integer, preserve, outputStream);
        } catch (IOException e) {
            throw new PapayaException(e);
        }
    }

    public void writeDup(long integer) throws PapayaException {
        String opcode = "dup";
        int opcodeValue = opcodeRegister.forName(opcode);
        builder.append("Op" + opcode.substring(0, 1).toUpperCase() + opcode.substring(1));
        builder.append("(").append("0x").append(Base16.encode(new byte[] { (byte) opcodeValue })).append(")\n");
        builder.append("\t").append(integer).append(" ");
        try {
            VarInt.writeCompactUInt32(integer, false, outputStream);
        } catch (IOException e) {
            throw new PapayaException(e);
        }
    }

    public void writeUInt32(long integer, boolean preserve) throws PapayaException {
        String opcode = "uconst";
        int opcodeValue = opcodeRegister.forName(opcode);
        builder.append("Op" + opcode.substring(0, 1).toUpperCase() + opcode.substring(1));
        builder.append("(").append("0x").append(Base16.encode(new byte[] { (byte) opcodeValue })).append(")\n");
        builder.append("\t").append(integer).append(" ").append(preserve).append("\n");
        try {
            VarInt.writeCompactUInt32(integer, preserve, outputStream);
        } catch (IOException e) {
            throw new PapayaException(e);
        }
    }

    public void writeUInt64(long integer, boolean preserve) throws PapayaException {
        String opcode = "uconst";
        int opcodeValue = opcodeRegister.forName(opcode);
        builder.append("Op" + opcode.substring(0, 1).toUpperCase() + opcode.substring(1));
        builder.append("(").append("0x").append(Base16.encode(new byte[] { (byte) opcodeValue })).append(")\n");
        builder.append("\t").append(integer).append(" ").append(preserve).append("\n");
        try {
            VarInt.writeCompactUInt32(integer, preserve, outputStream);
        } catch (IOException e) {
            throw new PapayaException(e);
        }
    }

    public void writeNInt256(BigInteger integer, boolean preserve) throws PapayaException {
        String opcode = "nconst256";
        int opcodeValue = opcodeRegister.forName(opcode);
        builder.append("Op" + opcode.substring(0, 1).toUpperCase() + opcode.substring(1));
        builder.append("(").append("0x").append(Base16.encode(new byte[] { (byte) opcodeValue })).append(")\n");
        builder.append("\t").append(integer).append(" ").append(preserve).append("\n");
        try {
            VarInt.writeCompactUint256(integer.not(), preserve, outputStream);
        } catch (IOException | AdeniumException e) {
            throw new PapayaException(e);
        }
    }

    public void writeUInt256(BigInteger integer, boolean preserve) throws PapayaException {
        String opcode = "uconst256";
        int opcodeValue = opcodeRegister.forName(opcode);
        builder.append("Op" + opcode.substring(0, 1).toUpperCase() + opcode.substring(1));
        builder.append("(").append("0x").append(Base16.encode(new byte[] { (byte) opcodeValue })).append(")\n");
        builder.append("\t").append(integer).append(" ").append(preserve).append("\n");
        try {
            VarInt.writeCompactUint256(integer, preserve, outputStream);
        } catch (IOException | AdeniumException e) {
            throw new PapayaException(e);
        }
    }

    public String getOpcodeString() {
        return builder.toString();
    }

    public byte[] getOpcodes() {
        return outputStream.toByteArray();
    }

    public void writeTuple(long integer) throws PapayaException {
        String opcode = "tuple";
        int opcodeValue = opcodeRegister.forName(opcode);
        builder.append("Op" + opcode.substring(0, 1).toUpperCase() + opcode.substring(1));
        builder.append("(").append("0x").append(Base16.encode(new byte[] { (byte) opcodeValue })).append(")\n");
        builder.append("\t").append(integer).append("\n");
        try {
            VarInt.writeCompactUInt32(integer, false, outputStream);
        } catch (IOException e) {
            throw new PapayaException(e);
        }
    }

    public void writeJnt(ProgramWriter writer) throws PapayaException {
        String opcode = "jnt";
        int opcodeValue = opcodeRegister.forName(opcode);
        builder.append("Op" + opcode.substring(0, 1).toUpperCase() + opcode.substring(1));
        builder.append("(").append("0x").append(Base16.encode(new byte[] { (byte) opcodeValue })).append(")");
        builder.append(" ").append(writer.getOpcodes().length).append(" ");
        builder.append("\n");

        try {
            VarInt.writeCompactUInt32(writer.getOpcodes().length, false, outputStream);

            builder.append(writer.builder);
            outputStream.write(writer.outputStream.toByteArray());
        } catch (IOException e) {
            throw new PapayaException(e);
        }
    }

    public void writeReturn(int amount) throws PapayaException {
        String opcode = "return";
        int opcodeValue = opcodeRegister.forName(opcode);
        builder.append("Op" + opcode.substring(0, 1).toUpperCase() + opcode.substring(1));
        builder.append("(").append("0x").append(Base16.encode(new byte[] { (byte) opcodeValue })).append(")\n");
        builder.append("\t").append(amount).append("\n");
        try {
            VarInt.writeCompactUInt32(amount, false, outputStream);
        } catch (IOException e) {
            throw new PapayaException(e);
        }
    }
}
