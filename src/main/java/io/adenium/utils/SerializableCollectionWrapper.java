package io.adenium.utils;

import io.adenium.exceptions.AdeniumException;
import io.adenium.serialization.SerializableI;
import io.adenium.core.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SerializableCollectionWrapper<T extends SerializableI> extends SerializableI {
    private Collection<T> collection;

    public SerializableCollectionWrapper(Collection<T> collection) {
        this.collection = collection;
    }

    @Override
    public void write(OutputStream stream) throws IOException, AdeniumException {
        VarInt.writeCompactUInt32(collection.iterator().next().getSerialNumber(), false, stream);
        VarInt.writeCompactUInt32(collection.size(), false, stream);
        for (T t : collection) {
            t.write(stream);
        }
    }

    @Override
    public void read(InputStream stream) throws IOException, AdeniumException {
        int slvn = VarInt.readCompactUInt32(false, stream);
        int size = VarInt.readCompactUInt32(false, stream);
        List<T> list = new ArrayList<>();
        for (int i = 0; i < size; i ++) {
            list.add(Context.getInstance().getSerialFactory().fromStream(slvn, stream));
        }

        this.collection = list;
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
        return (Type) new SerializableCollectionWrapper<T>(new ArrayList<>());
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(SerializableCollectionWrapper.class);
    }
}
