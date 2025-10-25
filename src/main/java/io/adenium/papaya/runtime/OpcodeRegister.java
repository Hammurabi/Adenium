package io.adenium.papaya.runtime;

import io.adenium.papaya.compiler.Struct;
import io.adenium.encoders.Base16;
import io.adenium.encoders.Base58;
import io.adenium.exceptions.InvalidTransactionException;
import io.adenium.exceptions.PapayaException;
import io.adenium.exceptions.UndefOpcodeException;
import io.adenium.utils.Utils;
import io.adenium.utils.VoidCallableThrowsTY;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class OpcodeRegister {
    private Map<String, OpcodeDefinition>     opcodeNameMap;
    private Map<Integer, OpcodeDefinition>    opcodeMap;
    private Set<OpcodeDefinition>             opcodeSet;

    public OpcodeRegister() {
        opcodeNameMap = new HashMap<>();
        opcodeMap = new HashMap<>();
        opcodeSet = new LinkedHashSet<>();
    }

    public static void register(OpcodeRegister opcodeRegister) {
        opcodeRegister.registerOp("halt", "stop virtual process (and sub-processes).", 1, 1, scope -> scope.stopProcesses(scope.getProgramCounter().nextByte()));
        opcodeRegister.registerOp("push", "push an empty value to the stack.", 1, scope -> scope.getStack().push(scope.getNullReference()));
        opcodeRegister.registerOp("pop", "pop the top element from the stack.", 1, scope -> scope.getStack().pop());
        opcodeRegister.registerOp("drop", "drop the top of the stack, undoes a 'rebase' operation.", 5, scope -> scope.drop());
        opcodeRegister.registerOp("return", "drop the top of the stack, undoes a 'rebase' operation, keeps the top element(s) of the stack.", 5, scope -> scope.dropKeep(scope.getProgramCounter().nextVarint32(false)));
        opcodeRegister.registerOp("destroy", "destroy contract and return all funds to provided address.", 100, Scope::destroyContract);

        opcodeRegister.registerOp("call", "pop the top stack element and call it.", 2, 4, scope -> {
            scope.rebase();
            scope.getStack().pop().call(scope);
        });

        opcodeRegister.registerOp("rbp", "replace the value of a pointer on the stack.", 2, scope -> scope.getStack().pop().set(scope.getStack().pop()));
        opcodeRegister.registerOp("load", "load a member from an offset.", 3, 2, scope -> scope.getStack().push(scope.getStack().pop().getMember(scope.getProgramCounter().nextMemberId(), scope.getStackTrace())));
        opcodeRegister.registerOp("store", "store a member to an offset.", 2, 2, scope -> scope.getStack().pop().setMember(scope.getProgramCounter().nextMemberId(), scope.getStack().pop(), scope.getStackTrace()));

        opcodeRegister.registerOp("loadstatic", "load a static member from an offset.", 3, 2,
                scope -> scope.getStack().push(scope.getStack().pop().getMember(scope.getProgramCounter().nextMemberId(), scope.getStackTrace())));
        opcodeRegister.registerOp("storestatic", "store a static member to an offset.", 2, 2,
                scope -> scope.getStack().pop().setMember(scope.getProgramCounter().nextMemberId(), scope.getStack().pop(), scope.getStackTrace()));

        opcodeRegister.registerOp("tuple", "creates a new tuple object of size 'n'.", 1, 25, scope -> scope.makeTuple(scope.getProgramCounter().nextVarint32(false)));
        opcodeRegister.registerOp("getfield", "load an object from an offset in array.", 2, 2, scope -> scope.getStack().pop().getAtIndex(scope.getStack().pop().asInt().intValue()));
        opcodeRegister.registerOp("setfield", "store an object to an offset in array.", 2, 2, scope -> scope.getStack().pop().setAtIndex(scope.getStack().pop().asInt().intValue(), scope.getStack().pop()));
        opcodeRegister.registerOp("append", "append an object to an array.", 2, scope -> scope.getStack().pop().append(scope.getStack().pop()));

        opcodeRegister.registerOp("pushdata", "push an array of bytes of length (6-31) into the stack.", true, 1, 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaByteArray(scope.getProgramCounter().next(scope.getProgramCounter().nextVarint32(false))))));
        opcodeRegister.registerOp("push20", "push an array of bytes of length (160) into the stack.", true, 20, 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaByteArray(scope.getProgramCounter().next(20)))));
        opcodeRegister.registerOp("push32", "push an array of bytes of length (256) into the stack.", true, 20, 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaByteArray(scope.getProgramCounter().next(32)))));

        opcodeRegister.registerOp("jmp", "jumps to a location in code", 1, scope -> scope.getProgramCounter().jump(scope.getProgramCounter().nextUnsignedShort()));
        opcodeRegister.registerOp("jnt", "branch operator, jumps if condition is not true.", 1, scope -> {
            if (!scope.getStack().pop().asBool()) {
                scope.getProgramCounter().skip(scope.getProgramCounter().nextProgramPointer());
            }
        });

        opcodeRegister.registerOp("const0", "push an integer with value '0' (unsigned).", 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(0, false))));
        opcodeRegister.registerOp("const1", "push an integer with value '1' (unsigned).", 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(1, false))));
        opcodeRegister.registerOp("const2", "push an integer with value '2' (unsigned).", 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(2, false))));
        opcodeRegister.registerOp("const3", "push an integer with value '3' (unsigned).", 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(3, false))));
        opcodeRegister.registerOp("const4", "push an integer with value '4' (unsigned).", 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(4, false))));
        opcodeRegister.registerOp("const5", "push an integer with value '5' (unsigned).", 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(5, false))));
        opcodeRegister.registerOp("const6", "push an integer with value '6' (unsigned).", 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(6, false))));
        opcodeRegister.registerOp("const7", "push an integer with value '7' (unsigned).", 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(7, false))));
        opcodeRegister.registerOp("const8", "push an integer with value '8' (unsigned).", 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(8, false))));
        opcodeRegister.registerOp("const9", "push an integer with value '9' (unsigned).", 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(9, false))));
        opcodeRegister.registerOp("const10", "push an integer with value '10' (unsigned).", 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(10, false))));
        opcodeRegister.registerOp("const11", "push an integer with value '11' (unsigned).", 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(11, false))));
        opcodeRegister.registerOp("const12", "push an integer with value '12' (unsigned).", 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(12, false))));
        opcodeRegister.registerOp("const13", "push an integer with value '13' (unsigned).", 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(13, false))));
        opcodeRegister.registerOp("const14", "push an integer with value '14' (unsigned).", 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(14, false))));
        opcodeRegister.registerOp("const15", "push an integer with value '15' (unsigned).", 1, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(15, false))));

        opcodeRegister.registerOp("nconst", "push a negative varint of size '5-61'.", 1, 2, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(scope.getProgramCounter().nextVarint64(false), false))));
        opcodeRegister.registerOp("nconst256", "push a negative varint of size '3-251'.", 1, 12, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(scope.getProgramCounter().nextVarint256(false), false))));
        opcodeRegister.registerOp("uconst", "push a varint of size '5-61' (unsigned).", 1, 2, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(scope.getProgramCounter().nextVarint64(false), false))));
        opcodeRegister.registerOp("uconst256", "push a varint of size '3-251' (unsigned).", 1, 12, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(scope.getProgramCounter().nextVarint256(false), false))));

        opcodeRegister.registerOp("const64", "push an integer of size '64' (signed).", 8, 4, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(scope.getProgramCounter().nextLong(), false))));
        opcodeRegister.registerOp("const128", "push an integer of size '128' integer (unsigned).", 16, 12, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(new BigInteger(1, scope.getProgramCounter().next(16)), false))));
        opcodeRegister.registerOp("const256", "push an integer of size '256' (unsigned).", 32, 24, scope -> scope.getStack().push(new DefaultHandler(new PapayaNumber(new BigInteger(1, scope.getProgramCounter().next(32)), false))));

        opcodeRegister.registerOp("fconst", "push a float of size '32' (unsigned).", 4, 1, scope -> {
            throw new PapayaException("float is not supported at the moment.");
        });
        opcodeRegister.registerOp("fconst64", "push a float of size '64' (unsigned).", 8, 1, scope -> {
            throw new PapayaException("float is not supported at the moment.");
        });
        opcodeRegister.registerOp("fconst256", "push a float of size '256' (unsigned).", 32, 1, scope -> {
            throw new PapayaException("float is not supported at the moment.");
        });

        opcodeRegister.registerOp("aconst200", "push an address of size '200'.", 25, 1, scope -> {
            throw new PapayaException("address is not supported at the moment.");
        });
        opcodeRegister.registerOp("aconst256", "push a hash of size '256'.", 32, 1, scope -> {
            throw new PapayaException("hash256 is not supported at the moment.");
        });

        opcodeRegister.registerOp("verify", "throws an 'InvalidTransactionException' if the top stack item is not true.", 1, Scope::verify);
//        opcodeRegister.registerOp("flipsign", "pop an object from the stack and reinterpret the most significant bit as a sign bit.", 1, scope -> scope.getStack().peek().flipSign());

        opcodeRegister.registerOp("add", "pop two objects from the stack and add them.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.Add));
        opcodeRegister.registerOp("sub", "pop two objects from the stack and sub them.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.Sub));
        opcodeRegister.registerOp("mul", "pop two objects from the stack and mul them.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.Mul));
        opcodeRegister.registerOp("div", "pop two objects from the stack and div them.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.Div));
        opcodeRegister.registerOp("mod", "pop two objects from the stack and mod them.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.Mod));
        opcodeRegister.registerOp("pow", "pop two objects from the stack and mod them.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.Pow));
        opcodeRegister.registerOp("and", "pop two objects from the stack and perform bitwise and on them.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.And));
        opcodeRegister.registerOp("or", "pop two objects from the stack and perform bitwise or on them.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.Or));
        opcodeRegister.registerOp("xor", "pop two objects from the stack and perform bitwise xor on them.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.Xor));
        opcodeRegister.registerOp("shf", "pop two objects from the stack and perform arithmetic shift on them.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.UnsignedShift));
        opcodeRegister.registerOp("rsh", "pop two objects from the stack right shift.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.RightShift));
        opcodeRegister.registerOp("lsh", "pop two objects from the stack left shift.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.LeftShift));
        opcodeRegister.registerOp("not", "pop an object from the stack and perform bitwise not on it.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.None));
        opcodeRegister.registerOp("ngt", "pop an object from the stack and perform logical not on it.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.Negate));

        opcodeRegister.registerOp("leq", "pop an object from the stack and perform <= on it.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.LessThanOrEqual));
        opcodeRegister.registerOp("geq", "pop an object from the stack and perform >= on it.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.GreaterThanOrEqual));
        opcodeRegister.registerOp("lst", "pop an object from the stack and perform < on it.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.LessThan));
        opcodeRegister.registerOp("gtn", "pop an object from the stack and perform > on it.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.GreaterThan));
        opcodeRegister.registerOp("eq", "pop an object from the stack and perform == on it.", 1, scope -> scope.callOperator(io.adenium.papaya.compiler.Struct.Operator.Equals));
        opcodeRegister.registerOp("neq", "pop an object from the stack and perform != on it.", 1, scope -> scope.callOperator(Struct.Operator.NotEquals));

        opcodeRegister.registerOp("dup", "duplicate the nth stack element (by reference).", 8, scope -> scope.getStack().dup(scope.getProgramCounter().nextVarint32(false)));
        opcodeRegister.registerOp("dup1", "duplicate the first stack element (by reference).", 1, scope -> scope.getStack().dup());
        opcodeRegister.registerOp("dup2", "duplicate the second stack element (by reference).", 1, scope -> scope.getStack().dup(2));
        opcodeRegister.registerOp("dup3", "duplicate the third stack element (by reference).", 1, scope -> scope.getStack().dup(3));
        opcodeRegister.registerOp("dup4", "duplicate the fourth stack element (by reference).", 1, scope -> scope.getStack().dup(4));
        opcodeRegister.registerOp("dup5", "duplicate the fifth stack element (by reference).", 1, scope -> scope.getStack().dup(5));
        opcodeRegister.registerOp("dup6", "duplicate the sixth stack element (by reference).", 1, scope -> scope.getStack().dup(6));
        opcodeRegister.registerOp("dup7", "duplicate the seventh stack element (by reference).", 1, scope -> scope.getStack().dup(7));
        opcodeRegister.registerOp("dup8", "duplicate the eighth stack element (by reference).", 1, scope -> scope.getStack().dup(8));
        opcodeRegister.registerOp("dup9", "duplicate the ninth stack element (by reference).", 1, scope -> scope.getStack().dup(9));
        opcodeRegister.registerOp("dup10", "duplicate the tenth stack element (by reference).", 1, scope -> scope.getStack().dup(10));
        opcodeRegister.registerOp("dup11", "duplicate the eleventh stack element (by reference).", 1, scope -> scope.getStack().dup(11));
        opcodeRegister.registerOp("dup12", "duplicate the twelfth stack element (by reference).", 1, scope -> scope.getStack().dup(12));
        opcodeRegister.registerOp("dup13", "duplicate the thirteenth stack element (by reference).", 1, scope -> scope.getStack().dup(13));
        opcodeRegister.registerOp("dup14", "duplicate the fourteenth stack element (by reference).", 1, scope -> scope.getStack().dup(14));
        opcodeRegister.registerOp("dup15", "duplicate the fifteenth stack element (by reference).", 1, scope -> scope.getStack().dup(15));
        opcodeRegister.registerOp("dup16", "duplicate the sixteenth stack element (by reference).", 1, scope -> scope.getStack().dup(16));

        opcodeRegister.registerOp("swap", "swap two objects (the 1st and nth) on the stack.", 8, scope -> scope.getStack().swap(1, scope.getProgramCounter().nextVarint32(false)));
        opcodeRegister.registerOp("swap1", "swap two objects (the 1st and 2nd) on the stack.", 1, scope -> scope.getStack().swap(1, 2));
        opcodeRegister.registerOp("swap2", "swap two objects (the 1st and 3rd) on the stack.", 1, scope -> scope.getStack().swap(1, 3));
        opcodeRegister.registerOp("swap3", "swap two objects (the 1st and 4th) on the stack.", 1, scope -> scope.getStack().swap(1, 4));
        opcodeRegister.registerOp("swap4", "swap two objects (the 1st and 5th) on the stack.", 1, scope -> scope.getStack().swap(1, 5));
        opcodeRegister.registerOp("swap5", "swap two objects (the 1st and 6th) on the stack.", 1, scope -> scope.getStack().swap(1, 6));
        opcodeRegister.registerOp("swap6", "swap two objects (the 1st and 7th) on the stack.", 1, scope -> scope.getStack().swap(1, 7));
        opcodeRegister.registerOp("swap7", "swap two objects (the 1st and 8th) on the stack.", 1, scope -> scope.getStack().swap(1, 8));
        opcodeRegister.registerOp("swap8", "swap two objects (the 1st and 9th) on the stack.", 1, scope -> scope.getStack().swap(1, 9));
        opcodeRegister.registerOp("swap9", "swap two objects (the 1st and 10th) on the stack.", 1, scope -> scope.getStack().swap(1, 10));
        opcodeRegister.registerOp("swap10", "swap two objects (the 1st and 11th) on the stack.", 1, scope -> scope.getStack().swap(1, 11));
        opcodeRegister.registerOp("swap11", "swap two objects (the 1st and 12th) on the stack.", 1, scope -> scope.getStack().swap(1, 12));
        opcodeRegister.registerOp("swap12", "swap two objects (the 1st and 13th) on the stack.", 1, scope -> scope.getStack().swap(1, 13));
        opcodeRegister.registerOp("swap13", "swap two objects (the 1st and 14th) on the stack.", 1, scope -> scope.getStack().swap(1, 14));
        opcodeRegister.registerOp("swap14", "swap two objects (the 1st and 15th) on the stack.", 1, scope -> scope.getStack().swap(1, 15));
        opcodeRegister.registerOp("swap15", "swap two objects (the 1st and 16th) on the stack.", 1, scope -> scope.getStack().swap(1, 16));
    }

    public byte[] parse(String asm) throws PapayaException, IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        String data[] = asm.replaceAll("\n", " ").replaceAll("\\s+", " ").split(" ");
        Iterator<String> iterator = Arrays.stream(data).iterator();

        while (iterator.hasNext()) {
            String opName   = iterator.next().replaceAll("^(Op)", "").toLowerCase();
            OpcodeDefinition opcode   = getOpcode(opName);
            outputStream.write(opcode.getIdentifier());

            if (opcode.hasVarargs()) {
                if (!iterator.hasNext()) {
                    throw new PapayaException("Reached EOF but expected argument(s) for '" + opName + "'.");
                }

                byte array[]    = null;
                String argument = iterator.next();

                if (argument.matches("\\d+")) {     // base 10 number
                    long arg = Long.parseLong(argument);

                    if (Long.toString(arg).equals(argument)) {
                        if (arg <= 255) {
                            array = new byte[] { (byte) arg };
                        } else if (arg <= 65535) {
                            array = Utils.takeApartChar((char) arg);
                        } else if (arg <= 16777215) {
                            array = Utils.takeApartInt24(arg);
                        } else if (arg <= 4294967295L) {
                            array = Utils.takeApart(arg);
                        } else {
                            array = Utils.takeApartLong(arg);
                        }
                    } else {
                        array = new BigInteger(argument).toByteArray();
                    }
                } else if (Base58.isEncoded(argument)) {  // base 58 value
                    array = Base58.decode(argument);
                } else if (Base16.isEncoded(argument)) {  // base 16 value
                    array = Base16.decode(argument);
                } else {
                    throw new PapayaException("Unknown format format for string '" + argument + "'.");
                }

                byte length[] = null;

                switch (opcode.getNumArgs()) {
                    case 1:
                        if (array.length > 255) {
                            throw new PapayaException("Opcode '" + opName + "' takes maximum length argument(s) of '" + opcode.getNumArgs() + "'.");
                        }

                        length = new byte[] { (byte) array.length };
                        break;
                    case 2:
                        if (array.length > 65535) {
                            throw new PapayaException("Opcode '" + opName + "' takes maximum length argument(s) of '" + opcode.getNumArgs() + "'.");
                        }

                        length = Utils.takeApartChar((char) array.length);
                        break;
                    case 3:
                        if (array.length > 16777215) {
                            throw new PapayaException("Opcode '" + opName + "' takes maximum length argument(s) of '" + opcode.getNumArgs() + "'.");
                        }

                        length = Utils.takeApartInt24((char) array.length);
                        break;
                    case 4:
                        length = Utils.takeApart((char) array.length);
                        break;
                    default:
                        throw new PapayaException("Unsupported vararg size not in range of '1' to '4'.");
                }

                outputStream.write(length);
                outputStream.write(array);
            } else if (opcode.getNumArgs() > 0) {
                byte array[]    = null;
                String argument = iterator.next();

                if (argument.matches("\\d+")) {     // base 10 number
                    long arg = Long.parseLong(argument);

                    if (Long.toString(arg).equals(argument)) {
                        if (arg <= 255 && opcode.getNumArgs() == 1) {
                            array = new byte[] { (byte) arg };
                        } else if (arg <= 65535 && opcode.getNumArgs() == 2) {
                            array = Utils.takeApartChar((char) arg);
                        } else if (arg <= 16777215 && opcode.getNumArgs() == 3) {
                            array = Utils.takeApartInt24(arg);
                        } else if (arg <= 4294967295L && opcode.getNumArgs() == 4) {
                            array = Utils.takeApart(arg);
                        } else if (arg <= Long.MAX_VALUE && opcode.getNumArgs() == 8) {
                            array = Utils.takeApartLong(arg);
                        } else {
                            throw new PapayaException("Opcode '" + opName + "' takes '" + opcode.getNumArgs() + "' argument(s).");
                        }
                    } else {
                        array = new BigInteger(argument).toByteArray();
                    }
                } else if (Base58.isEncoded(argument)) {  // base 58 value
                    array = Base58.decode(argument);
                } else if (Base16.isEncoded(argument)) {  // base 16 value
                    array = Base16.decode(argument);
                } else {
                    throw new PapayaException("Unknown format format for string '" + argument + "'.");
                }

                if (array.length != opcode.getNumArgs()) {
                    throw new PapayaException("Opcode '" + opName + "' takes '" + opcode.getNumArgs() + "' argument(s).");
                }

                outputStream.write(array);
            }
        }

        outputStream.flush();
        outputStream.close();
        return outputStream.toByteArray();
    }

    // register an opcode into the vm
    public OpcodeRegister registerOp(String name, String description, long weight, VoidCallableThrowsTY<Scope, PapayaException, InvalidTransactionException> callable) {
        return registerOp(name, description, 0, weight, callable);
    }

    public OpcodeRegister registerOp(String name, String description, int numArgs, long weight, VoidCallableThrowsTY<Scope, PapayaException, InvalidTransactionException> callable) {
        return registerOp(name, description, false, numArgs, weight, callable);
    }

    public OpcodeRegister registerOp(String name, String description, boolean vararg, int numArgs, long weight, VoidCallableThrowsTY<Scope, PapayaException, InvalidTransactionException> callable) {
        OpcodeDefinition opcode = new OpcodeDefinition(name, description, "", opcodeSet.size(), vararg, numArgs, callable, weight);
        opcodeNameMap.put(name, opcode);
        opcodeMap.put(opcode.getIdentifier(), opcode);
        opcodeSet.add(opcode);

        return this;
    }

    private OpcodeDefinition getOpcode(String opName) throws UndefOpcodeException {
        if (opcodeNameMap.containsKey(opName)) {
            return opcodeNameMap.get(opName);
        }

        throw new UndefOpcodeException(opName);
    }

    public OpcodeDefinition getOpcode(int opcode) throws UndefOpcodeException {
        if (opcodeMap.containsKey(opcode)) {
            return opcodeMap.get(opcode);
        }

        throw new UndefOpcodeException(opcode);
    }

    public int opCount() {
        return opcodeSet.size();
    }

    public int forName(String op) {
        return opcodeNameMap.get(op).getIdentifier();
    }
}
