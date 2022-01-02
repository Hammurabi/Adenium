//package org.wolkenproject.fastnio;
//
//import org.wolkenproject.utils.Utils;
//
//class UnsafeBuffer extends Buffer {
//    private final long buffer;
//    private final long length;
//
//    UnsafeBuffer(int size) {
//        this.buffer = UnsafeInstance.allocateMemory(size);
//        this.length = size;
//        setPosition(0);
//    }
//
//    @Override
//    public int getLength() {
//        return (int) length;
//    }
//
//    @Override
//    public void put(int offset, int value) {
//         UnsafeInstance.putByte(offset + buffer, (byte) value);
//    }
//
//    @Override
//    public void put(int offset, byte[] value) {
//        for (int i = 0; i < value.length; i ++) {
//            put(offset + i, value[i]);
//        }
//    }
//
//    @Override
//    public void putShort(int offset, int value) {
//        UnsafeInstance.putShort(offset + buffer, (short) value);
//    }
//
//    @Override
//    public void putChar(int offset, int value) {
//        UnsafeInstance.putChar(offset + buffer, (char) value);
//    }
//
//    @Override
//    public void putInt(int offset, int value) {
//        UnsafeInstance.putInt(offset + buffer, value);
//    }
//
//    @Override
//    public void putLong(int offset, long value) {
//        UnsafeInstance.putLong(offset + buffer, value);
//    }
//
//    @Override
//    public int get(int offset) {
//        return UnsafeInstance.getByte(offset + buffer);
//    }
//
//    @Override
//    public void get(int offset, byte[] out) {
//        for (int i = 0; i < out.length; i ++) {
//            out[i] = (byte) get(offset + i);
//        }
//    }
//
//    @Override
//    public int getShort(int offset) {
//        return UnsafeInstance.getShort(offset + buffer);
//    }
//
//    @Override
//    public int getChar(int offset) {
//        return UnsafeInstance.getChar(offset + buffer);
//    }
//
//    @Override
//    public int getInt(int offset) {
//        return UnsafeInstance.getInt(offset + buffer);
//    }
//
//    @Override
//    public long getLong(int offset) {
//        return UnsafeInstance.getLong(offset + buffer);
//    }
//}
