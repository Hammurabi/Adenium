package io.adenium.papaya.parser;

import io.adenium.utils.Utils;

import java.io.IOException;
import java.io.InputStream;

public class Program extends InputStream {
    private byte    buffer[];
    private int     index;

    public Program(byte buffer[], int index) {
        this.buffer = buffer;
        this.index  = index;
    }

    @Override
    public int read() throws IOException {
        return checkBounds(index ++);
    }

    public short readShort() throws IOException {
        return Utils.makeShort(read(), read());
    }

    public char readChar() throws IOException {
        return Utils.makeChar(read(), read());
    }

    public int readInt() throws IOException {
        return Utils.makeInt(read(), read(), read(), read());
    }

    public long readLong() throws IOException {
        return Utils.makeLong(read(), read(), read(), read(), read(), read(), read(), read());
    }

    private int checkBounds(int index) throws IOException {
        if (index >= buffer.length) {
            throw new IOException("end of buffer reached.");
        }

        return Byte.toUnsignedInt(buffer[index]);
    }

    public short readShort(int index) throws IOException {
        return Utils.makeShort(checkBounds(index), checkBounds(index + 1));
    }

    public char readChar(int index) throws IOException {
        return Utils.makeChar(checkBounds(index), checkBounds(index + 1));
    }

    public int readInt(int index) throws IOException {
        return Utils.makeInt(checkBounds(index), checkBounds(index + 1), checkBounds(index + 2), checkBounds(index + 3));
    }

    public long readLong(int index) throws IOException {
        return Utils.makeLong(checkBounds(index), checkBounds(index + 1), checkBounds(index + 2), checkBounds(index + 3), checkBounds(index + 4), checkBounds(index + 5), checkBounds(index + 6), checkBounds(index + 7));
    }

    public void setPosition(int index) {
        this.index = index;
    }

    public int remaining() {
        return buffer.length - index;
    }

    public boolean hasRemaining() {
        return remaining() > 0;
    }

    public int length() {
        return buffer.length;
    }

    public int getPosition() {
        return index;
    }
}
