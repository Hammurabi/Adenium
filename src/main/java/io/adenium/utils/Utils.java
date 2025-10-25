package io.adenium.utils;

import io.adenium.encoders.Base16;
import io.adenium.encoders.Base58;
import io.adenium.serialization.SerializableI;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Queue;
import java.util.Set;

public class Utils {
    public static final byte[] concatenate(byte[]...arrays)
    {
        int size = 0;
        for(byte[] array : arrays)
            size += array.length;

        byte concatenated[] = new byte[size];

        int index = 0;

        for(byte[] array : arrays)
        {
            System.arraycopy(array, 0, concatenated, index, array.length);

            index += array.length;
        }

        return concatenated;
    }

    public static void println(byte bytes[]) {
        for (int i = 0; i < bytes.length; i ++) {
            System.out.print(bytes[i] + " ");
        }

        System.out.println();
    }

    public static byte[] takeApart(BigInteger integer) {
        byte bytes[] = integer.toByteArray();
        
        if (bytes.length == 0) {
            return new byte[1];
        }

        if (bytes[0] == 0 && bytes.length > 1) {
            byte temp[] = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, temp, 0, temp.length);
            bytes = temp;
        }

        return bytes;
    }

    public static byte[] takeApart(BigInteger integer, int minLength) {
        return conditionalExpand(minLength, takeApart(integer));
    }

    public static short makeShort(int b1, int b0) {
        return (short) (((b1 & 0xff) <<  8) | ((b0 & 0xff)));
    }

    public static char makeChar(int b1, int b0) {
        return (char) (((b1 & 0xff) <<  8) | ((b0 & 0xff)));
    }

    public static int makeInt(int b3, int b2, int b1, int b0) {
        return (((b3 & 0xff) << 24) |
                ((b2 & 0xff) << 16) |
                ((b1 & 0xff) <<  8) |
                ((b0 & 0xff)      ));
    }

    public static long makeLong(int b7, int b6, int b5, int b4,
                                int b3, int b2, int b1, int b0)
    {
        return ((((long)b7 & 0xff) << 56) |
                (((long)b6 & 0xff) << 48) |
                (((long)b5 & 0xff) << 40) |
                (((long)b4 & 0xff) << 32) |
                (((long)b3 & 0xff) << 24) |
                (((long)b2 & 0xff) << 16) |
                (((long)b1 & 0xff) <<  8) |
                (((long)b0 & 0xff)      ));
    }

    public static int makeInt24(byte[] trim) {
        return makeInt((byte) 0, trim[0], trim[1], trim[2]);
    }

    public static int makeInt(byte[] trim) {
        return makeInt(trim[0], trim[1], trim[2], trim[3]);
    }

    public static int makeInt(byte[] trim, int off) {
        return makeInt(trim[off], trim[off + 1], trim[off + 2], trim[off + 3]);
    }

    public static long makeLong(byte[] trim) {
        return makeLong(trim, 0);
    }

    public static long makeLong(byte[] trim, int offset) {
        if (trim.length < 8) {
            trim = concatenate(new byte[8 - trim.length], trim);
        }

        return makeLong(trim[offset], trim[offset + 1], trim[offset + 2], trim[offset + 3], trim[offset + 4], trim[offset + 5], trim[offset + 6], trim[offset + 7]);
    }

    public static long makeLongUnsafe(byte[] trim) {
        return makeLong(trim[0], trim[1], trim[2], trim[3], trim[4], trim[5], trim[6], trim[7]);
    }

    public static byte[] trim(byte[] bytes, int offset, int length) {
        byte new_bytes[]    = new byte[length];

        int free            = 0;

        for(int index = offset; index < (offset + length); index ++)
            new_bytes[free ++] = bytes[index];
        return new_bytes;
    }

    public static int[] trim(int[] array, int offset, int length) {
        int new_array[]    = new int[length];

        int free            = 0;

        for(int index = offset; index < (offset + length); index ++)
            new_array[free ++] = array[index];
        return new_array;
    }

    public static boolean equals(byte[] trim, byte[] trim1) {
        if(trim.length != trim1.length) return false;

        for(int i = 0; i < trim1.length; i ++)
            if(trim[i] != trim1[i]) return false;

        return true;
    }

    public static byte[] int24(int i)
    {
        return new byte[] {
                (byte) ((i >>> 16) & 0xFF),
                (byte) ((i >>>  8) & 0xFF),
                (byte) ((i) & 0xFF)};
    }

    public static byte[] appendIntegerToBytes(byte data[], int integer)
    {
        return concatenate(data, new byte[] {
                (byte) ((integer >>> 24) & 0xFF),
                (byte) ((integer >>> 16) & 0xFF),
                (byte) ((integer >>> 8) & 0xFF),
                (byte) ((integer) & 0xFF)});
    }

    public static long getTimeHours(int numHours, short mins, short seconds)
    {
        return (numHours * (3600) + (mins * 60) + seconds) * 1000L;
    }

    public static long getTimeHours(int numHours)
    {
        return numHours * 3600_000L;
    }

    public static long getTimeMinutes(int numMinutes)
    {
        return numMinutes * 60_000L;
    }

    public static byte[] reverseBytes(byte[] bytes) {
        byte[] buf = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            buf[i] = bytes[bytes.length - 1 - i];
        return buf;
    }

    public static byte[] takeApartShort(long integer) {
        return new byte[] {
                (byte) ((integer >>> 8) & 0xFF),
                (byte) ((integer) & 0xFF)};
    }

    public static byte[] takeApartChar(long integer) {
        return new byte[] {
                (byte) ((integer >>> 8) & 0xFF),
                (byte) ((integer) & 0xFF)};
    }

    public static byte[] takeApart(long integer) {
        return new byte[] {
                (byte) ((integer >>> 24) & 0xFF),
                (byte) ((integer >>> 16) & 0xFF),
                (byte) ((integer >>> 8) & 0xFF),
                (byte) ((integer) & 0xFF)};
    }

    public static byte[] appendLongToBytes(byte[] data, long integer) {
        return concatenate(data, new byte[] {
                (byte) ((integer >>> 56) & 0xFF),
                (byte) ((integer >>> 48) & 0xFF),
                (byte) ((integer >>> 40) & 0xFF),
                (byte) ((integer >>> 32) & 0xFF),
                (byte) ((integer >>> 24) & 0xFF),
                (byte) ((integer >>> 16) & 0xFF),
                (byte) ((integer >>>  8) & 0xFF),
                (byte) ((integer) & 0xFF)});
    }

    public static byte[] takeApartLong(long integer)
    {
        return new byte[] {
                (byte) ((integer >>> 56) & 0xFF),
                (byte) ((integer >>> 48) & 0xFF),
                (byte) ((integer >>> 40) & 0xFF),
                (byte) ((integer >>> 32) & 0xFF),
                (byte) ((integer >>> 24) & 0xFF),
                (byte) ((integer >>> 16) & 0xFF),
                (byte) ((integer >>>  8) & 0xFF),
                (byte) ((integer) & 0xFF)};
    }

    public static byte[] takeApartInt40(long integer)
    {
        return new byte[] {
                (byte) ((integer >>> 32) & 0xFF),
                (byte) ((integer >>> 24) & 0xFF),
                (byte) ((integer >>> 16) & 0xFF),
                (byte) ((integer >>>  8) & 0xFF),
                (byte) ((integer) & 0xFF)};
    }

    public static byte[] takeApartInt48(long integer)
    {
        return new byte[] {
                (byte) ((integer >>> 40) & 0xFF),
                (byte) ((integer >>> 32) & 0xFF),
                (byte) ((integer >>> 24) & 0xFF),
                (byte) ((integer >>> 16) & 0xFF),
                (byte) ((integer >>>  8) & 0xFF),
                (byte) ((integer) & 0xFF)};
    }

    public static byte[] takeApartInt56(long integer)
    {
        return new byte[] {
                (byte) ((integer >>> 48) & 0xFF),
                (byte) ((integer >>> 40) & 0xFF),
                (byte) ((integer >>> 32) & 0xFF),
                (byte) ((integer >>> 24) & 0xFF),
                (byte) ((integer >>> 16) & 0xFF),
                (byte) ((integer >>>  8) & 0xFF),
                (byte) ((integer) & 0xFF)};
    }

    public static byte[] fromSet(Set<byte[]> hashes) {
        byte bytes[] = new byte[0];

        for (byte [] data : hashes) bytes = concatenate(bytes, data);

        return bytes;
    }

    public static byte[] takeApart(char[] password) {
        ByteBuffer buffer = ByteBuffer.allocate(password.length * 2);
        for (char chr : password) buffer.putChar(chr);
        buffer.flip();

        return buffer.array();
    }

    public static byte[] fillArray(byte[] chars, byte c) {
        Arrays.fill(chars, c);
        return chars;
    }

    public static char[] fillArray(char[] chars, char c) {
        Arrays.fill(chars, c);
        return chars;
    }

    public static char[] makeChars(byte[] trim) {
        ByteBuffer charBuffer = ByteBuffer.allocate(trim.length);
        charBuffer.put(trim);
        charBuffer.flip();
        char chars[] = new char[trim.length / 2];
        for (int i = 0; i < chars.length; i ++) {
            chars[i] = charBuffer.getChar();
        }

        return chars;
    }

    public static boolean empty(byte[] data) {
        for (byte b : data) {
            if (b != 0) {
                return false;
            }
        }

        return true;
    }

    public static byte[] decodeString(String encoded) {
        if (Base58.isEncoded(encoded)) {
            return Base58.decode(encoded);
        } else if (Base16.isEncoded(encoded)) {
            return Base16.decode(encoded);
        }

        return null;
    }

    public static void writeInt(int integer, OutputStream stream) throws IOException {
        stream.write((byte) ((integer >>> 24) & 0xFF));
        stream.write((byte) ((integer >>> 16) & 0xFF));
        stream.write((byte) ((integer >>> 8) & 0xFF));
        stream.write((byte) ((integer) & 0xFF));
    }

    public static int readInt(InputStream stream) throws IOException {
        byte buffer[] = new byte[4];
        SerializableI.checkFullyRead(stream.read(buffer), 4);

        return makeInt(buffer);
    }

    public static long readLong(InputStream stream) throws IOException {
        byte buffer[] = new byte[8];
        SerializableI.checkFullyRead(stream.read(buffer), 8);

        return makeLong(buffer);
    }

    public static void writeLong(long integer, OutputStream stream) throws IOException {
        stream.write((byte) ((integer >>> 56) & 0xFF));
        stream.write((byte) ((integer >>> 48) & 0xFF));
        stream.write((byte) ((integer >>> 40) & 0xFF));
        stream.write((byte) ((integer >>> 32) & 0xFF));
        stream.write((byte) ((integer >>> 24) & 0xFF));
        stream.write((byte) ((integer >>> 16) & 0xFF));
        stream.write((byte) ((integer >>>  8) & 0xFF));
        stream.write((byte) ((integer) & 0xFF));
    }

    public static void writeUnsignedInt16(int integer, OutputStream stream) throws IOException {
        stream.write((byte) ((integer >>> 8) & 0xFF));
        stream.write((byte) ((integer) & 0xFF));
    }

    public static int timestampInSeconds() {
        return (int) (System.currentTimeMillis() / 1000) - 1048035600;
    }

    public static byte[] conditionalExpand(int newLength, byte[] bytes) {
        if (newLength == bytes.length) {
            return bytes;
        }

        return concatenate(new byte[newLength - bytes.length], bytes);
    }

    public static byte[] pad(int padCount, byte[] bytes) {
        return concatenate(new byte[padCount], bytes);
    }

    public static byte[] append(byte[] bytes, int count) {
        return concatenate(bytes, new byte[count]);
    }

    public static byte[] pad(int padCount, int padValue, byte[] bytes) {
        return concatenate(fillArray(new byte[padCount], (byte) padValue), bytes);
    }

    public static int getBit(long byt, int position)
    {
        return (int) ((byt >>> position) & 1);
    }

    public static int setBit(int byt, int position)
    {
        return byt | 1 << position;
    }

    public static int clearBit(int byt, int position)
    {
        return byt & ~(1 << position);
    }

    public static int toggleBit(int byt, int position)
    {
        return byt ^ ~(1 << position);
    }

    public static int setBit(int byt, int position, int value)
    {
        return byt ^ (-(value & 1) ^ byt) & (1 << position);
    }

    public static int makeByte(byte[] buffer) {
        int value = 0;
        for (int i = 0; i < buffer.length; i ++) {
            value = setBit(value, i, buffer[i]);
        }

        return value;
    }

    public static <T> T[] prepend(T element, T[] elements) {
        Object n[] = new Object[elements.length + 1];

        n[0] = element;

        for (int i = 1; i < n.length; i ++) {
            n[i] = elements[i - 1];
        }

        return (T[]) n;
    }

    public static byte[] takeApart(int[] array) {
        byte bArray[] = new byte[0];

        for (int i = 0; i < array.length; i ++) {
            bArray  = concatenate(bArray, takeApart(array[i]));
        }

        return bArray;
    }

    public static byte[] takeApartInt24(long integer) {
        return new byte[] {
                (byte) ((integer >>> 16) & 0xFF),
                (byte) ((integer >>> 8) & 0xFF),
                (byte) ((integer) & 0xFF)};
    }

    public static String toString(Object argument) {
        if (argument == null) {
            return "null";
        }

        return argument.toString();
    }

    public static byte[] toBytesPadded(BigInteger value, int length) {
        byte[] result = new byte[length];
        byte[] bytes = value.toByteArray();

        int bytesLength;
        int srcOffset;
        if (bytes[0] == 0) {
            bytesLength = bytes.length - 1;
            srcOffset = 1;
        } else {
            bytesLength = bytes.length;
            srcOffset = 0;
        }

        if (bytesLength > length) {
            return bytes;
        }

        int destOffset = length - bytesLength;
        System.arraycopy(bytes, srcOffset, result, destOffset, bytesLength);
        return result;
    }

    public static boolean isEmpty(byte[] empty) {
        for (byte b : empty) {
            if (b != 0) {
                return false;
            }
        }

        return true;
    }

    public static double log2(double x) {
        return (Math.log(x) / Math.log(2));
    }

    // return the minimum number of bits required to represent this number
    public static int numBitsRequired(long x) {
        return (int) (Math.floor(log2(Math.max(x, 1))) + 1);
    }

    public static void skipBytes(InputStream stream, int numBytes) throws IOException {
        for (int i = 0; i < numBytes; i ++) {
            stream.read();
        }
    }

    public static byte[] calculateMerkleRoot(byte a[], byte b[]) {
        return HashUtil.sha256d(Utils.concatenate(a, b));
    }

    public static byte[] calculateMerkleRoot(Queue<byte[]> hashes) {
        while (hashes.size() > 1) {
            hashes.add(calculateMerkleRoot(hashes.poll(), hashes.poll()));
        }

        return hashes.poll();
    }

    public static JSONObject jsonDate(long ms) {
        final String daysOfWeek[] = {
              "Sunday",
              "Monday",
              "Tuesday",
              "Wednesday",
              "Thursday",
              "Friday",
              "Saturday",
        };
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(ms);
        JSONObject jsonDate = new JSONObject();
        jsonDate.put("year", calendar.get(Calendar.YEAR));
        jsonDate.put("month", calendar.get(Calendar.MONTH));
        jsonDate.put("day", calendar.get(Calendar.DAY_OF_MONTH));
        jsonDate.put("dow", daysOfWeek[calendar.get(Calendar.DAY_OF_WEEK) - 1]);
        jsonDate.put("hour", calendar.get(Calendar.HOUR_OF_DAY));
        jsonDate.put("minute", calendar.get(Calendar.MINUTE));
        jsonDate.put("second", calendar.get(Calendar.SECOND));
        jsonDate.put("millisecond", ms);
        return jsonDate;
    }
}
