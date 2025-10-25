package io.adenium.utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class BitInputStream extends InputStream {
    private byte    array[];
    private int     index;

    public BitInputStream(byte array[]) {
        this.array  = array;
        this.index  = 0;
    }

    @Override
    public int read() throws IOException {
        int location = index ++;
        if (location / 8 >= array.length) {
            throw new EOFException("reached end of stream.");
        }

        int byteAddress = location / 8;
        int bitAddress  = location % 8;

        return Utils.getBit(array[byteAddress], bitAddress);
    }

    public int readByte() throws IOException {
        byte buffer[] = new byte[8];

        int read = read(buffer);
        if (read < 8) {
            throw new IOException("not enough bits remaining.");
        }

        return Utils.makeByte(buffer);
    }

    public boolean hasRemaining() {
        return index < array.length * 8;
    }

    public int remainingBits() {
        return (array.length * 8) - index;
    }

    public int remainingBytes() {
        return remainingBits() / 8;
    }

    public byte[] getBitsAsByteArray(int length) throws IOException {
        byte array[] = new byte[(int) Math.ceil(length / 8.0)];
        int offset = (array.length * 8) - length;

        for (int i = offset; i < array.length * 8; i ++) {
            int byteAddress = i / 8;
            int bitAddress  = i % 8;
            int read = read();

            if (read < 0) {
                throw new IOException("not enough bits remaining.");
            }

            array[byteAddress] = (byte) Utils.setBit(array[byteAddress], bitAddress, read);
        }

        return array;
    }

    public byte[] readBitsAsByteArray(byte array[]) throws IOException {
        int offset = (array.length * 8) - array.length;

        for (int i = offset; i < array.length * 8; i ++) {
            int byteAddress = i / 8;
            int bitAddress  = i % 8;
            int read = read();
            if (read < 0) {
                throw new IOException("not enough bits remaining.");
            }

            Utils.setBit(array[byteAddress], bitAddress, read);
        }

        return array;
    }
}
