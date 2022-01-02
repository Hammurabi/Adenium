package io.adenium;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.adenium.exceptions.WolkenException;
import io.adenium.utils.Utils;
import io.adenium.utils.VarInt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public class VarIntTests {
    @Test
    public void testReadWriteLossy32() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int ints[] = new int[] {
                0, 1, 2, 5, 10, 20, 22, 30, 32, 40, 60, 120, 12000, 24000, 124000, 1000000, 1000000000
        };

        byte empty[] = new byte[12];

        for (int i : ints) {
            VarInt.writeCompactUInt32(i, false, outputStream);
            outputStream.write(empty);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        for (int i : ints) {
            Assertions.assertEquals(i, VarInt.readCompactUInt32(false, inputStream));
            Assertions.assertEquals(inputStream.read(empty), empty.length);
            Assertions.assertTrue(Utils.isEmpty(empty), "array should consist of zeros.");
        }
    }

    @Test
    public void testReadWriteLossless32() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int ints[] = new int[] {
                0, 1, 2, 5, 10, 20, 22, 30, 32, 40, 60, 120, 12000, 24000, 124000, 1000000, 1000000000
        };

        byte empty[] = new byte[12];

        for (int i : ints) {
            VarInt.writeCompactUInt32(i, true, outputStream);
            outputStream.write(empty);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        for (int i : ints) {
            Assertions.assertEquals(i, VarInt.readCompactUInt32(true, inputStream));
            Assertions.assertEquals(inputStream.read(empty), empty.length);
            Assertions.assertTrue(Utils.isEmpty(empty), "array should consist of zeros.");
        }
    }

    @Test
    public void testReadWriteLossy64() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        long ints[] = new long[] {
                0, 1, 2, 5, 10, 20, 22, 30, 32, 40, 60, 120, 12000, 24000, 124000, 1000000, 1000000000, 1000000000L, 90000000000L, 190000000000L, 3190000000000L, 203190000000000L, 1355203190000000000L,
                21_000_000__00_000_000_000L
        };

        byte empty[] = new byte[12];

        for (long i : ints) {
            VarInt.writeCompactUInt64(i, false, outputStream);
            outputStream.write(empty);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        for (long i : ints) {
            Assertions.assertEquals(i, VarInt.readCompactUInt64(false, inputStream));
            Assertions.assertEquals(inputStream.read(empty), empty.length);
            Assertions.assertTrue(Utils.isEmpty(empty), "array should consist of zeros.");
        }
    }

    @Test
    public void testReadWriteLossless64() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        long ints[] = new long[] {
                0, 1, 2, 5, 10, 20, 22, 30, 31, 32, 40, 60, 120, 12000, 24000, 124000, 1000000, 1000000000, 1000000000L, 90000000000L, 190000000000L, 3190000000000L, 203190000000000L, 1355203190000000000L,
                21_000_000__00_000_000_000L
        };

        byte empty[] = new byte[12];

        for (long i : ints) {
            VarInt.writeCompactUInt64(i, true, outputStream);
            outputStream.write(empty);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        for (long i : ints) {
            Assertions.assertEquals(i, VarInt.readCompactUInt64(true, inputStream));
            Assertions.assertEquals(inputStream.read(empty), empty.length);
            Assertions.assertTrue(Utils.isEmpty(empty), "array should consist of zeros.");
        }
    }

    @Test
    public void testReadWriteLossy128() throws IOException, WolkenException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        BigInteger ints[] = new BigInteger[] {
                BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE.shiftLeft(2), BigInteger.ONE.shiftLeft(3), BigInteger.ONE.shiftLeft(4).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(4), BigInteger.ONE.shiftLeft(8),
                BigInteger.ONE.shiftLeft(12), BigInteger.ONE.shiftLeft(16), BigInteger.ONE.shiftLeft(20), BigInteger.ONE.shiftLeft(24), BigInteger.ONE.shiftLeft(28),
                BigInteger.ONE.shiftLeft(32), BigInteger.ONE.shiftLeft(36), BigInteger.ONE.shiftLeft(40).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(46), BigInteger.ONE.shiftLeft(57),
                BigInteger.ONE.shiftLeft(63), BigInteger.ONE.shiftLeft(64), BigInteger.ONE.shiftLeft(120).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(120), BigInteger.ONE.shiftLeft(124).subtract(BigInteger.ONE)
        };

        byte empty[] = new byte[12];

        for (BigInteger i : ints) {
            VarInt.writeCompactUint128(i, false, outputStream);
            outputStream.write(empty);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        for (BigInteger i : ints) {
            Assertions.assertEquals(i, VarInt.readCompactUint128(false, inputStream));
            Assertions.assertEquals(inputStream.read(empty), empty.length);
            Assertions.assertTrue(Utils.isEmpty(empty), "array should consist of zeros.");
        }
    }

    @Test
    public void testReadWriteLossless128() throws IOException, WolkenException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        BigInteger ints[] = new BigInteger[] {
                BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE.shiftLeft(2), BigInteger.ONE.shiftLeft(3), BigInteger.ONE.shiftLeft(4).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(4), BigInteger.ONE.shiftLeft(8),
                BigInteger.ONE.shiftLeft(12), BigInteger.ONE.shiftLeft(16), BigInteger.ONE.shiftLeft(20), BigInteger.ONE.shiftLeft(24), BigInteger.ONE.shiftLeft(28),
                BigInteger.ONE.shiftLeft(32), BigInteger.ONE.shiftLeft(36), BigInteger.ONE.shiftLeft(40).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(46), BigInteger.ONE.shiftLeft(57),
                BigInteger.ONE.shiftLeft(63), BigInteger.ONE.shiftLeft(64), BigInteger.ONE.shiftLeft(120).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(120),
                BigInteger.ONE.shiftLeft(125).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(126).subtract(BigInteger.ONE),
                BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE)
        };

        byte empty[] = new byte[12];

        for (BigInteger i : ints) {
            VarInt.writeCompactUint128(i, true, outputStream);
            outputStream.write(empty);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        for (BigInteger i : ints) {
            Assertions.assertEquals(i, VarInt.readCompactUint128(true, inputStream));
            Assertions.assertEquals(inputStream.read(empty), empty.length);
            Assertions.assertTrue(Utils.isEmpty(empty), "array should consist of zeros.");
        }
    }

    @Test
    public void testReadWriteLossy256() throws IOException, WolkenException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        BigInteger ints[] = new BigInteger[] {
                BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE.shiftLeft(2), BigInteger.ONE.shiftLeft(3), BigInteger.ONE.shiftLeft(4).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(4), BigInteger.ONE.shiftLeft(8),
                BigInteger.ONE.shiftLeft(12), BigInteger.ONE.shiftLeft(16), BigInteger.ONE.shiftLeft(20), BigInteger.ONE.shiftLeft(24), BigInteger.ONE.shiftLeft(28),
                BigInteger.ONE.shiftLeft(32), BigInteger.ONE.shiftLeft(36), BigInteger.ONE.shiftLeft(40).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(46), BigInteger.ONE.shiftLeft(57),
                BigInteger.ONE.shiftLeft(63), BigInteger.ONE.shiftLeft(64), BigInteger.ONE.shiftLeft(120).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(120),
                BigInteger.ONE.shiftLeft(125).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(126).subtract(BigInteger.ONE),
                BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE),
                BigInteger.ONE.shiftLeft(129), BigInteger.ONE.shiftLeft(130), BigInteger.ONE.shiftLeft(150).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(160),
                BigInteger.ONE.shiftLeft(190).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(200).subtract(BigInteger.ONE),
                BigInteger.ONE.shiftLeft(220).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(251).subtract(BigInteger.ONE)
        };

        byte empty[] = new byte[12];

        for (BigInteger i : ints) {
            VarInt.writeCompactUint256(i, false, outputStream);
            outputStream.write(empty);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        for (BigInteger i : ints) {
            Assertions.assertEquals(i, VarInt.readCompactUint256(false, inputStream));
            Assertions.assertEquals(inputStream.read(empty), empty.length);
            Assertions.assertTrue(Utils.isEmpty(empty), "array should consist of zeros.");
        }
    }

    @Test
    public void testReadWriteLossless256() throws IOException, WolkenException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        BigInteger ints[] = new BigInteger[] {
                BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE.shiftLeft(2), BigInteger.ONE.shiftLeft(3), BigInteger.ONE.shiftLeft(4).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(4), BigInteger.ONE.shiftLeft(8),
                BigInteger.ONE.shiftLeft(12), BigInteger.ONE.shiftLeft(16), BigInteger.ONE.shiftLeft(20), BigInteger.ONE.shiftLeft(24), BigInteger.ONE.shiftLeft(28),
                BigInteger.ONE.shiftLeft(32), BigInteger.ONE.shiftLeft(36), BigInteger.ONE.shiftLeft(40).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(46), BigInteger.ONE.shiftLeft(57),
                BigInteger.ONE.shiftLeft(63), BigInteger.ONE.shiftLeft(64), BigInteger.ONE.shiftLeft(120).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(120),
                BigInteger.ONE.shiftLeft(125).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(126).subtract(BigInteger.ONE),
                BigInteger.ONE.shiftLeft(127).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE),
                BigInteger.ONE.shiftLeft(129), BigInteger.ONE.shiftLeft(130), BigInteger.ONE.shiftLeft(150).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(160),
                BigInteger.ONE.shiftLeft(190).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(200).subtract(BigInteger.ONE),
                BigInteger.ONE.shiftLeft(220).subtract(BigInteger.ONE), BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE)
        };

        byte empty[] = new byte[12];

        for (BigInteger i : ints) {
            VarInt.writeCompactUint256(i, true, outputStream);
            outputStream.write(empty);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        for (BigInteger i : ints) {
            Assertions.assertEquals(i, VarInt.readCompactUint256(true, inputStream));
            Assertions.assertEquals(inputStream.read(empty), empty.length);
            Assertions.assertTrue(Utils.isEmpty(empty), "array should consist of zeros.");
        }
    }
}
