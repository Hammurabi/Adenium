package io.adenium.fastnio;

//import jdk.internal.misc.Unsafe;

//import java.lang.reflect.Field;

public abstract class Buffer {
//    static final Unsafe UnsafeInstance = getUnsafe();
    private int position;

//    private static final Unsafe getUnsafe() {
//        try {
//            Field f = Unsafe.class.getDeclaredField("theUnsafe");
//            f.setAccessible(true);
//            return  (Unsafe) f.get(null);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }

    public static Buffer wrap(byte array[]) {
        return new HeapBuffer(array);
    }

    public static Buffer createBuffer(int length) {
        return new HeapBuffer(length);
    }

    public static Buffer createUnsafeBuffer(int length) {
//        return new UnsafeBuffer(length);
        return null;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public void flip() {
        setPosition(0);
    }

    public int remaining() {
        return getLength() - getPosition();
    }

    public abstract int getLength();

    public void put(int value) {
        put(value, position ++);
    }

    public void put(byte value[]) {
        put(position, value);
        setPosition(getPosition() + value.length);
    }

    public void putShort(int value) {
        putShort(position, value);
        setPosition(getPosition() + 2);
    }

    public void putChar(int value) {
        putChar(position, value);
        setPosition(getPosition() + 2);
    }

    public void putInt(int value) {
        putInt(position, value);
        setPosition(getPosition() + 4);
    }

    public void putLong(long value) {
        putLong(position, value);
        setPosition(getPosition() + 8);
    }

    public int get() {
        return get(position ++);
    }

    public void get(byte out[]) {
        get(position, out);
        setPosition(getPosition() + out.length);
    }

    public int getShort() {
        try {
            return getShort(position);
        } finally {
            setPosition(getPosition() + 2);
        }
    }
    public int getChar() {
        try {
            return getChar(position);
        } finally {
            setPosition(getPosition() + 2);
        }
    }

    public int getInt() {
        try {
            return getInt(position);
        } finally {
            setPosition(getPosition() + 4);
        }
    }

    public long getLong() {
        try {
            return getLong(position);
        } finally {
            setPosition(getPosition() + 8);
        }
    }

    public abstract void put(int offset, int value);
    public abstract void put(int offset, byte value[]);
    public abstract void putShort(int offset, int value);
    public abstract void putChar(int offset, int value);
    public abstract void putInt(int offset, int value);
    public abstract void putLong(int offset, long value);

    public abstract int get(int offset);
    public abstract void get(int offset, byte out[]);
    public abstract int getShort(int offset);
    public abstract int getChar(int offset);
    public abstract int getInt(int offset);
    public abstract long getLong(int offset);
}
