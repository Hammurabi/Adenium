package io.adenium.papaya.compiler;

import io.adenium.utils.VarInt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public enum StructureType {
    None,
    ModuleType,
    ContractType,
    ClassType,
    StructType;


    public StructureType read(InputStream stream) throws IOException {
        int type = VarInt.readCompactUInt32(false, stream);

        switch (type) {
            case 0:
                return None;
            case 1:
                return ModuleType;
            case 2:
                return ContractType;
            case 3:
                return ClassType;
            case 4:
                return StructType;
            default:
                throw new IOException("'" + type + "' is an invalid index of 'StructureType'.");
        }
    }


    public static void write(StructureType type, OutputStream stream) throws IOException {
        switch (type) {
            case None:
                VarInt.writeCompactUInt32(0, false, stream);
                break;
            case ModuleType:
                VarInt.writeCompactUInt32(1, false, stream);
                break;
            case ContractType:
                VarInt.writeCompactUInt32(2, false, stream);
                break;
            case ClassType:
                VarInt.writeCompactUInt32(3, false, stream);
                break;
            case StructType:
                VarInt.writeCompactUInt32(4, false, stream);
                break;
        }
    }
}
