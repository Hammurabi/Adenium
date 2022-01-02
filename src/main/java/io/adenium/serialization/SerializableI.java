package io.adenium.serialization;

import io.adenium.core.Context;
import io.adenium.exceptions.WolkenException;
import io.adenium.utils.HashUtil;
import io.adenium.utils.VarInt;

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public abstract class SerializableI {
    // this function gets called when the Serializable object is serialized locally
    public void serialize(OutputStream stream) throws IOException, WolkenException {
        VarInt.writeCompactUInt32(getSerialNumber(), false, stream);
        write(stream);
    }

    // this function gets called when the Serializable object is sent over network
    public void serializeOverNetwork(OutputStream stream) throws IOException, WolkenException {
        serialize(stream);
    }

    // this function gets called when the Serializable object is sent over network
    public void sendOverNetwork(OutputStream stream) throws IOException, WolkenException {
        write(stream);
    }

    public abstract void write(OutputStream stream) throws IOException, WolkenException;
    public abstract void read(InputStream stream) throws IOException, WolkenException;

    public <T> T fromBytes(byte bytes[]) throws IOException, WolkenException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        read(inputStream);
        return (T) this;
    }

    public byte[] asByteArray() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            write(outputStream);
            outputStream.flush();
            outputStream.close();

            return outputStream.toByteArray();
        } catch (IOException | WolkenException e) {
            return null;
        }
    }

    public byte[] asByteArray(int compressionLevel) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DeflaterOutputStream outputStream = new DeflaterOutputStream(byteArrayOutputStream, new Deflater(compressionLevel));
            write(outputStream);
            outputStream.flush();
            outputStream.close();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException | WolkenException e) {
            return null;
        }
    }

    public byte[] asSerializedArray() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            serialize(outputStream);
            outputStream.flush();
            outputStream.close();

            return outputStream.toByteArray();
        } catch (IOException | WolkenException e) {
            return null;
        }
    }

    public <Type extends SerializableI> Type makeCopy() throws IOException, WolkenException {
        byte array[] = asSerializedArray();
        BufferedInputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(array));
        Type t = Context.getInstance().getSerialFactory().fromStream(inputStream);
        inputStream.close();

        return t;
    }

    public abstract <Type extends SerializableI> Type newInstance(Object... object) throws WolkenException;

    public byte[] checksum() {
        return HashUtil.hash160(asByteArray());
    }

    public abstract int getSerialNumber();

    public static void checkFullyRead(int result, int expected) throws IOException {
        if (result != expected) {
            throw new IOException("expected '" + expected + "' bytes but only received '" + result + "'");
        }
    }

    public static int checkNotEOF(int read) throws IOException {
        if (read < 0) {
            throw new IOException("end of file reached.");
        }

        return read;
    }

    public <T> T fromCompressed(byte[] compressed) throws IOException, WolkenException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressed);
        InflaterInputStream inputStream = new InflaterInputStream(byteArrayInputStream);
        read(inputStream);
        inputStream.close();

        return (T) this;
    }
}