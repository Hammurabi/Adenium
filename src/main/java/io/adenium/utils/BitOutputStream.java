package io.adenium.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BitOutputStream extends OutputStream {
    private ByteArrayOutputStream   stream;
    private byte                    current;
    private int                     index;

    public BitOutputStream() {
        this.stream = new ByteArrayOutputStream();
        this.current = 0;
        this.index  = 0;
    }

    @Override
    public void write(int i) throws IOException {
        current = (byte) Utils.setBit(current, index ++, i);

        if (index == 8) {
            stream.write(current);
            current = 0;
            index = 0;
        }
    }

    public void write(int x, int bits) throws IOException {
        for (int i = 0; i < bits; i ++) {
            write(Utils.getBit(x, i));
        }
    }

    @Override
    public void flush() throws IOException {
        super.flush();
        stream.write(current);
        current = 0;
        index = 0;
    }

    public void writeBitsFromBytes(byte[] bytes, int length) throws IOException {
        for (int i = 0; i < length; i ++) {
            int byteAddress = i / 8;
            int bitAddress  = i % 8;

            int bit = Utils.getBit(bytes[byteAddress], bitAddress);
            write(bit);
        }
    }

    public byte[] toByteArray() {
        return stream.toByteArray();
    }
}
