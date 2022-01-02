package io.adenium.core;

import io.adenium.utils.Utils;

import java.math.BigInteger;
import java.util.Arrays;

public class Int256 {
    public static final Int256 Zero = new Int256(0, false);
    public static final Int256 One  = new Int256(1, false);
    public static final Int256 Two  = new Int256(2, false);
    public static final Int256 Ten  = new Int256(10, false);
    public static final Int256 Max  = new Int256(Utils.fillArray(new byte[32], (byte) 0xFF), false);

    public int      data[];
    public boolean  signed;

    public Int256(long value) {
        this(value, true);
    }

    public Int256(long value, boolean signed) {
        this(convertLong(value), signed);
    }

    public Int256(byte array[], boolean signed) {
        this(convertArray(array), signed);
    }

    private static int[] convertArray(byte[] array) {
        int ints[]  = new int[8];
        ints[0]     = Utils.makeInt(array);
        ints[1]     = Utils.makeInt(array, 4);
        ints[2]     = Utils.makeInt(array, 8);
        ints[3]     = Utils.makeInt(array, 12);
        ints[4]     = Utils.makeInt(array, 16);
        ints[5]     = Utils.makeInt(array, 20);
        ints[6]     = Utils.makeInt(array, 24);
        ints[7]     = Utils.makeInt(array, 28);

        return ints;
    }

    private static int[] convertLong(long value) {
        int result[] = new int[8];
        byte bytes[] = Utils.takeApartLong(value);

        result[6]   = Utils.makeInt(bytes, 0);
        result[7]   = Utils.makeInt(bytes, 4);

        return result;
    }

    private static long convertInts(int result[]) {
        byte bytes0[] = Utils.takeApart(result[6]);
        byte bytes1[] = Utils.takeApart(result[7]);

        return Utils.makeLong(Utils.concatenate(bytes0, bytes1));
    }

    private Int256(int data[], boolean signed) {
        this.data   = data;
        this.signed = signed;
    }

    private static int[] add(int x[], int y[]) {
        int xIndex = x.length;
        int yIndex = y.length;
        int[] result = new int[8];
        long sum = 0L;

        while(yIndex > 0) {
            --xIndex;
            long var10000 = (long)x[xIndex] & 4294967295L;
            --yIndex;
            sum = var10000 + ((long)y[yIndex] & 4294967295L) + (sum >>> 32);
            result[xIndex] = (int)sum;
        }

        boolean carry;
        for(carry = sum >>> 32 != 0L; xIndex > 0 && carry; carry = (result[xIndex] = x[xIndex] + 1) == 0) {
            --xIndex;
        }

        while(xIndex > 0) {
            --xIndex;
            result[xIndex] = x[xIndex];
        }

        // check for sign later
        if (carry) {
            for (int i = 0; i < 8; i ++) {
                result[i] = 0;
            }
        }

        return result;
    }

    private static int[] mul(int x[], int y[]) {
        int xstart  = x.length - 1;
        int ystart  = y.length - 1;
        int z[]       = new int[16];

        long carry = 0L;
        int i = ystart;

        int j;
        for(j = ystart + 1 + xstart; i >= 0; --j) {
            long product = ((long)y[i] & 4294967295L) * ((long)x[xstart] & 4294967295L) + carry;
            z[j] = (int)product;
            carry = product >>> 32;
            --i;
        }

        z[xstart] = (int)carry;

        for(i = xstart - 1; i >= 0; --i) {
            carry = 0L;
            j = ystart;

            for(int k = ystart + 1 + i; j >= 0; --k) {
                long product = ((long)y[j] & 4294967295L) * ((long)x[i] & 4294967295L) + ((long)z[k] & 4294967295L) + carry;
                z[k] = (int)product;
                carry = product >>> 32;
                --j;
            }

            z[i] = (int) carry;
        }

        int result[] = new int[8];
        System.arraycopy(z, 8, result, 0, 8);

        return result;
    }

    private static int[] div(int x[], int y[]) {
        BigInteger f;
        int xstart  = x.length - 1;
        int ystart  = y.length - 1;
        int z[]       = new int[16];

        long carry = 0L;
        int i = ystart;

        int j;
        for(j = ystart + 1 + xstart; i >= 0; --j) {
            long product = ((long)y[i] & 4294967295L) * ((long)x[xstart] & 4294967295L) + carry;
            z[j] = (int)product;
            carry = product >>> 32;
            --i;
        }

        z[xstart] = (int)carry;

        for(i = xstart - 1; i >= 0; --i) {
            carry = 0L;
            j = ystart;

            for(int k = ystart + 1 + i; j >= 0; --k) {
                long product = ((long)y[j] & 4294967295L) * ((long)x[i] & 4294967295L) + ((long)z[k] & 4294967295L) + carry;
                z[k] = (int)product;
                carry = product >>> 32;
                --j;
            }

            z[i] = (int) carry;
        }

        int result[] = new int[8];
        System.arraycopy(z, 8, result, 0, 8);

        return result;
    }

    public Int256 add(long number) {
        return new Int256(add(data, convertLong(number)), false);
    }

    public Int256 add(Int256 other) {
        return new Int256(add(data, other.data), false);
    }

    public Int256 software_adder(long number, boolean signed) {
        Int256 number256 = new Int256(convertLong(number), signed);

        if (signed) {
            return sub(number256);
        }

        return software_adder(number256);
    }

    public Int256 software_adder(Int256 other) {
        int carry       = 0;
        int result[]    = new int[8];

        for (int x = 0; x < 8; x ++) {
            int i = 7 - x;

            for (int b = 0; b < 32; b ++) {
                int bit0    = Utils.getBit(data[i], b);
                int bit1    = Utils.getBit(other.data[i], b);

                int sum0    = (bit0 ^ bit1);
                int sum1    = sum0 ^ carry;
                carry       = (bit0 & bit1) | (carry & sum0);

                result[i]   = Utils.setBit(result[i], b, sum1);
            }
        }

        return new Int256(result, !(other.signed & signed));
    }

    public Int256 mul(Int256 other) {
        return new Int256(mul(data, other.data), other.signed || signed);
    }

    public Int256 sub(Int256 other) {
        int result[]    = new int[8];

        for (int i = 0; i < 8; i ++) {
            result[i]   = ~other.data[i];
        }

        return software_adder(new Int256(result, !(other.signed & signed)).add(1));
    }

    public int[] getData() {
        return data;
    }

    public long asLong() {
        return convertInts(data);
    }

    public int asInt() {
        return data[7];
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Int256 self = new Int256(Arrays.copyOf(data, 8), signed);
        Int256 base = new Int256(10, false);

        do {
            Int256 remainder = self.mod(base);
            builder.append(remainder.asInt());
            self = self.div(base);
        } while (self.asInt() > 0);

        String string = builder.reverse().toString();//new StringBuilder(builder.reverse().toString().replaceAll("^(0)+", "")).reverse().toString();
        if (string.isEmpty()) {
            return "0";
        }

        return string;
                //new BigInteger(1, Utils.takeApart(data)).toString();
    }

    public int compare(Int256 other) {
        for(int i = 0; i < 8; ++i) {
            int a = data[i];
            int b = other.data[i];
            if (a != b) {
                return ((long)a & 4294967295L) < ((long)b & 4294967295L) ? -1 : 1;
            }
        }

        return 0;
    }

    public boolean isGreaterThan(Int256 other) {
        for(int i = 0; i < 8; ++i) {
            int a = data[i];
            int b = other.data[i];

            if (a != b) {
                return ((long)a & 4294967295L) > ((long)b & 4294967295L);
            }
        }

        return false;
    }

    public boolean isGreaterThanOrEqual(Int256 other) {
        for(int i = 0; i < 8; ++i) {
            int a = data[i];
            int b = other.data[i];

            if (a != b) {
                return ((long)a & 4294967295L) > ((long)b & 4294967295L);
            }
        }

        return true;
    }

    public boolean isLessThan(Int256 other) {
        for(int i = 0; i < 8; ++i) {
            int a = data[i];
            int b = other.data[i];

            if (a != b) {
                return ((long)a & 4294967295L) < ((long)b & 4294967295L);
            }
        }

        return false;
    }

    public boolean isLessThanOrEqual(Int256 other) {
        for(int i = 0; i < 8; ++i) {
            int a = data[i];
            int b = other.data[i];

            if (a != b) {
                return ((long)a & 4294967295L) <= ((long)b & 4294967295L);
            }
        }

        return true;
    }

    public Int256 div(Int256 other) {
        byte result[] = new BigInteger(getSignum(), Utils.takeApart(data)).divide(new BigInteger(other.getSignum(), Utils.takeApart(other.data))).toByteArray();
        if (result.length > 32) {
            byte temp[] = new byte[32];
            System.arraycopy(result, result.length - 32, temp, 0, 32);
            result = temp;
        } else if (result.length < 32) {
            result = Utils.pad(32 - result.length, result);
        }
        return new Int256(result, signed || other.signed);
    }

    public Int256 mod(Int256 other) {
        byte result[] = new BigInteger(getSignum(), Utils.takeApart(data)).mod(new BigInteger(other.getSignum(), Utils.takeApart(other.data))).toByteArray();
        if (result.length > 32) {
            byte temp[] = new byte[32];
            System.arraycopy(result, result.length - 32, temp, 0, 32);
            result = temp;
        } else if (result.length < 32) {
            result = Utils.pad(32 - result.length, result);
        }

        return new Int256(result, signed || other.signed);
    }

    public Int256 shiftLeft(int shift) {
        return new Int256(shiftLeft(data, shift), signed);
    }

    private static int[] shiftLeft(int[] mag, int n) {
        int nInts = n >>> 5;
        int nBits = n & 31;
        int magLen = mag.length;
        int[] newMag = null;

        if (nBits == 0) {
            newMag = new int[magLen + nInts];
            System.arraycopy(mag, 0, newMag, 0, magLen);
        } else {
            int i = 0;
            int nBits2 = 32 - nBits;
            int highBits = mag[0] >>> nBits2;
            if (highBits != 0) {
                newMag = new int[magLen + nInts + 1];
                newMag[i++] = highBits;
            } else {
                newMag = new int[magLen + nInts];
            }

            int j;
            for(j = 0; j < magLen - 1; newMag[i++] = mag[j++] << nBits | mag[j] >>> nBits2) {
            }

            newMag[i] = mag[j] << nBits;
        }

        int result[] = newMag;

        if (newMag.length > 8) {
            result = new int[8];
            System.arraycopy(newMag, newMag.length - 8, result, 0, 8);
        }

        return result;
    }

    public int getSignum() {
        if (signed) {
            if (Utils.getBit(data[0], 32) == 1) {
                return -1;
            }
        }

        for (int i = 0; i < 8; i ++) {
            if (data[i] != 0) {
                return 1;
            }
        }

        return 0;
    }
}
