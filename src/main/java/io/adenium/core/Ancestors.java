package io.adenium.core;

import io.adenium.exceptions.AdeniumException;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.VarInt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Set;

public class Ancestors extends SerializableI {
    private Set<byte[]> hashes;
    private byte        hash[];

    public Ancestors(byte hash[]) {
        hashes = new LinkedHashSet<>();
        this.hash = hash;
    }

    public void fill(BlockIndex tip) {
        int latest      = tip.getHeight() - 1;
        int earliest    = 0;
        int total       = latest;
        int mid         = total / 2;
        int mid_up      = mid + (total / 4);
        int mid_bt      = mid - (total / 4);

        byte hash[] = Context.getInstance().getDatabase().findBlockHash(latest);
        if (hash != null) {
            hashes.add(hash);
        }

        hash = Context.getInstance().getDatabase().findBlockHash(mid_up);
        if (hash != null) {
            hashes.add(hash);
        }

        hash = Context.getInstance().getDatabase().findBlockHash(mid);
        if (hash != null) {
            hashes.add(hash);
        }

        hash = Context.getInstance().getDatabase().findBlockHash(mid_bt);
        if (hash != null) {
            hashes.add(hash);
        }

        hash = Context.getInstance().getDatabase().findBlockHash(earliest);
        if (hash != null) {
            hashes.add(hash);
        }
    }

    public byte[] findCommon() {
        for (byte[] hash : hashes) {
            if (Context.getInstance().getDatabase().checkBlockExists(hash)) {
                return hash;
            }
        }

        return null;
    }

    @Override
    public void write(OutputStream stream) throws IOException, AdeniumException {
        stream.write(hash);
        VarInt.writeCompactUInt32(hashes.size(), false, stream);
        for (byte hash[] : hashes) {
            stream.write(hash);
        }
    }

    @Override
    public void read(InputStream stream) throws IOException, AdeniumException {
        int length = VarInt.readCompactUInt32(false, stream);

        for (int i = 0; i < length; i ++) {
            byte hash[] = new byte[Block.UniqueIdentifierLength];
            stream.read(hash);
            hashes.add(hash);
        }
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
        return (Type) new Ancestors(new byte[Block.UniqueIdentifierLength]);
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(Ancestors.class);
    }
}
