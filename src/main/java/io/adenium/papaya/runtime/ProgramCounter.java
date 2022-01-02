package io.adenium.papaya.runtime;

import io.adenium.encoders.Base16;
import io.adenium.exceptions.EmptyProgramCounterException;
import io.adenium.exceptions.PapayaException;
import io.adenium.exceptions.UndefOpcodeException;
import io.adenium.exceptions.AdeniumException;
import io.adenium.papaya.parser.Program;
import io.adenium.utils.ByteArray;
import io.adenium.utils.Utils;
import io.adenium.utils.VarInt;

import java.io.IOException;
import java.math.BigInteger;

public class ProgramCounter {
    private Program             program;
    private OpcodeRegister      register;

    public ProgramCounter(Program program, OpcodeRegister register) {
        this.program = program;
        this.register= register;
        program.setPosition(0);
    }

    public int nextByte() throws EmptyProgramCounterException {
        try {
            return program.read();
        } catch (IOException e) {
            throw new EmptyProgramCounterException();
        }
    }

    public BigInteger nextVarint256(boolean preserveAllBits) throws EmptyProgramCounterException {
        try {
            return VarInt.readCompactUint256(preserveAllBits, program);
        } catch (IOException | AdeniumException e) {
            throw new EmptyProgramCounterException();
        }
    }

    public BigInteger nextVarint128(boolean preserveAllBits) throws EmptyProgramCounterException {
        try {
        return VarInt.readCompactUint128(preserveAllBits, program);
    } catch (IOException | AdeniumException e) {
        throw new EmptyProgramCounterException();
    }
    }

    public long nextVarint64(boolean preserveAllBits) throws EmptyProgramCounterException {
        try {
        return VarInt.readCompactUInt64(preserveAllBits, program);
    } catch (IOException e) {
        throw new EmptyProgramCounterException();
        }
    }

    public int nextVarint32(boolean preserveAllBits) throws EmptyProgramCounterException {
        try {
            return VarInt.readCompactUInt32(preserveAllBits, program);
        } catch (IOException e) {
            throw new EmptyProgramCounterException();
        }
    }

    public int nextShort() throws EmptyProgramCounterException {
        if (remaining() >= 2) {
            try {
                return program.readShort();
            } catch (IOException e) {
                throw new EmptyProgramCounterException();
            }
        }

        throw new EmptyProgramCounterException();
    }

    public int nextProgramPointer() throws EmptyProgramCounterException {
        try {
            return VarInt.readCompactUInt32(false, program);
        } catch (IOException e) {
            throw new EmptyProgramCounterException();
        }
    }

    public int nextUnsignedShort() throws EmptyProgramCounterException {
        if (remaining() >= 2) {
            try {
                return program.readChar();
            } catch (IOException e) {
                throw new EmptyProgramCounterException();
            }
        }

        throw new EmptyProgramCounterException();
    }

    public int nextInt24() throws EmptyProgramCounterException {
        if (remaining() >= 3) {
            try {
                return Utils.makeInt((byte) 0, program.read(), program.read(), program.read());
            } catch (IOException e) {
                throw new EmptyProgramCounterException();
            }
        }

        throw new EmptyProgramCounterException();
    }

    public int nextInt() throws EmptyProgramCounterException {
        if (remaining() >= 4) {
            try {
                return program.readInt();
            } catch (IOException e) {
                throw new EmptyProgramCounterException();
            }
        }

        throw new EmptyProgramCounterException();
    }

    public long nextLong() throws EmptyProgramCounterException {
        if (remaining() >= 8) {
            try {
                return program.readLong();
            } catch (IOException e) {
                throw new EmptyProgramCounterException();
            }
        }

        throw new EmptyProgramCounterException();
    }

    public byte[] next(int length) throws EmptyProgramCounterException {
        if (remaining() >= length) {
            byte array[] = new byte[length];
            try {
                program.read(array);
            } catch (IOException e) {
                throw new EmptyProgramCounterException();
            }

            return array;
        }

        throw new EmptyProgramCounterException();
    }

    public int remaining() {
        return program.remaining();
    }

    public OpcodeDefinition next() throws UndefOpcodeException, EmptyProgramCounterException {
        if (program.hasRemaining()) {
            int nextOp = 0;

            try {
                nextOp = program.read();
            } catch (IOException e) {
                throw new EmptyProgramCounterException();
            }

            return register.getOpcode(nextOp);
        }

        throw new EmptyProgramCounterException();
    }

    public boolean hasNext() {
        return program.hasRemaining();
    }

    public void jump(int jumpLoc) throws PapayaException {
        if (program.length() < jumpLoc) {
            throw new PapayaException("invalid jump location.");
        }

        program.setPosition(jumpLoc);
    }

    public void skip(int skip) throws PapayaException {
        if (program.length() < program.getPosition() + skip) {
            throw new PapayaException("invalid jump location.");
        }

        program.setPosition(program.getPosition() + skip);
    }

    public String hexDump() throws PapayaException {
        StringBuilder builder   = new StringBuilder();
        int lineLength = 0;

        final int strip     = 20;
        final int indent    = 2;

        for (int i = 0; i < strip; i ++) {
            lineLength ++;
            builder.append("----");
        }

        builder.append("\n ");

        while (hasNext()) {
            OpcodeDefinition next = next();
            String hexCode = Base16.encode(new byte[] {(byte) next.getIdentifier()});
            lineLength += hexCode.length() + 1;
            builder.append(hexCode).append(" ");

            if (lineLength % strip == 0) {
                builder.append("\n ");
            } else if (lineLength % indent == 0) {
                builder.append("\t");
            }

            if (next.hasVarargs()) {
                int length = 0;

                switch (next.getNumArgs()) {
                    case 1:
                        length = nextByte();
                        break;
                    case 2:
                        length = nextUnsignedShort();
                        break;
                    case 3:
                        length = nextInt24();
                        break;
                    case 4:
                        length = nextInt();
                        break;
                    default:
                        throw new PapayaException("Opcode corrupt.");
                }

                String hex = Base16.encode(next(length));
                for (int i = 0; i < hex.length(); i += 2) {
                    builder.append(hex.substring(i, i + 2)).append(" ");
                    lineLength += 3;

                    if (lineLength % strip == 0) {
                        builder.append("\n ");
                    } else if (lineLength % indent == 0) {
                        builder.append("\t");
                    }
                }
            } else if (next.getNumArgs() > 0) {
                String hex = Base16.encode(next(next.getNumArgs()));
                for (int i = 0; i < hex.length(); i += 2) {
                    builder.append(hex.substring(i, i + 2)).append(" ");
                    lineLength += 3;

                    if (lineLength % strip == 0) {
                        builder.append("\n ");
                    } else if (lineLength % indent == 0) {
                        builder.append("\t");
                    }
                }
            }
        }

        return builder.toString();
    }

    public ByteArray nextMemberId() throws EmptyProgramCounterException {
        try {
            return ByteArray.wrap(VarInt.readCompactUint256Bytes(false, program));
        } catch (IOException e) {
            throw new EmptyProgramCounterException();
        }
    }
}
