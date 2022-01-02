package io.adenium.utils;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ByteArray implements Serializable {
    private static final long serialVersionUID = 4624519240501334525L;
    private final byte array[];

    public ByteArray(byte hash[]) {
        this.array = hash;
    }

    public byte[] getArray() {
        return array;
    }

    public int length() {
        return length();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ByteArray byteArray = (ByteArray) o;
        return Arrays.equals(array, byteArray.array);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(array);
    }

    public static final ByteArray wrap(byte array[]) {
        return new ByteArray(array);
    }

    public static final ByteArray wrap(String string) {
        return wrap(string.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isNull() {
        return array == null || array.length == 0;
    }
}
