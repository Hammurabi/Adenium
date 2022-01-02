package io.adenium.utils;

import io.adenium.encoders.Base16;
import io.adenium.exceptions.WolkenException;
import io.adenium.core.BlockHeader;
import io.adenium.core.Context;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

public class ChainMath {
    public static BigInteger x256 = new BigInteger("2").pow(256);

    public static long coinFromRaw(String raw) throws ArithmeticException {
        return new BigDecimal(raw).multiply(new BigDecimal(Context.getInstance().getContextParams().getOneCoin()), MathContext.UNLIMITED).longValueExact();
    }

    public static String rawFromCoin(long coin) throws ArithmeticException {
        return Long.toString(new BigDecimal(coin).divide(new BigDecimal(Context.getInstance().getContextParams().getOneCoin()), RoundingMode.DOWN).longValueExact());
    }

    public static boolean validSolution(byte solution[], int bits) {
        return new BigInteger(1, solution).compareTo(new BigInteger(1, targetBytesFromBits(bits))) < 0;
    }

    public static boolean lessThan(byte a[], byte b[]) {
        if (a.length < b.length) {
            int diff = b.length - a.length;
            for (int i = 0; i < diff; i++) {
                if (Byte.toUnsignedInt(b[i]) != 0) {
                    return true;
                }
            }

            for (int i = 0; i < a.length; i++) {
                if (Byte.toUnsignedInt(a[i]) >= Byte.toUnsignedInt(b[i + diff])) {
                    return false;
                } else {
                    return true;
                }
            }
        } else if (b.length < a.length) {
            int diff = a.length - b.length;
            for (int i = 0; i < diff; i++) {
                if (Byte.toUnsignedInt(a[i]) != 0) {
                    return false;
                }
            }

            for (int i = 0; i < b.length; i++) {
                if (Byte.toUnsignedInt(a[i + diff]) >= Byte.toUnsignedInt(b[i])) {
                    return false;
                } else {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < a.length; i++) {
                if (Byte.toUnsignedInt(a[i]) >= Byte.toUnsignedInt(b[i])) {
                    return false;
                } else return true;
            }
        }

        return true;
    }

    public static byte[] targetFromBits(byte bits[]) throws WolkenException {
        int length = Byte.toUnsignedInt(bits[0]);
        if (length > 32)
            throw new WolkenException("invalid target bits '" + Base16.encode(bits) + "'.");

        byte target[] = Utils.fillArray(new byte[length], (byte) 255);

        target[0] = bits[1];
        target[1] = bits[2];
        target[2] = bits[3];

        return target;
    }

    public static byte[] targetFromBits256(byte bits[]) throws WolkenException {
        byte target[] = targetFromBits(bits);

        if (target.length < 32) {
            return Utils.concatenate(new byte[32 - target.length], target);
        }

        return target;
    }

    public static byte[] targetBytesFromBits(int bits) {
        byte target[] = new byte[32];
        int offset = 32 - ((bits >>> 0x18) & 0x1D);
        target[offset + 0] = (byte) ((bits >>> 0x10) & 0xFF);
        target[offset + 1] = (byte) ((bits >>> 0x08) & 0xFF);
        target[offset + 2] = (byte) ((bits) & 0xFF);

        return target;
    }

    public static BigInteger targetIntegerFromBits(int bits) {
        return new BigInteger(1, targetBytesFromBits(bits));
    }

    public static long getReward(long height) {
        long numberOfHalvings = (height + 1) / Context.getInstance().getContextParams().getHalvingRate();

        if (numberOfHalvings >= 37) {
            return 0;
        }

        long d = 1 << numberOfHalvings;

        return Context.getInstance().getContextParams().getMaxReward() / d;
    }

    public static BigInteger getTotalWork(byte[] bits) throws WolkenException {
        return x256.divide(new BigInteger(1, targetFromBits(bits)));
    }

    public static int calculateNewTarget(BlockHeader block, int height) {
        if (height == 0) {
            return Context.getInstance().getContextParams().getDefaultBits();
        }

        if (height % Context.getInstance().getContextParams().getDifficultyAdjustmentThreshold() == 0) {
            BlockHeader earliest = null;

            int previousBlockHeight = height - Context.getInstance().getContextParams().getDifficultyAdjustmentThreshold();

            if (previousBlockHeight >= 0) {
                earliest = Context.getInstance().getDatabase().findBlockHeader(previousBlockHeight);
            }

            return generateTargetBits(block, earliest);
        }

        return block.getBits();
    }

    public static int generateTargetBits(BlockHeader latest, BlockHeader earliest) {
        //calculate the target time for 1800 blocks.
        long timePerDiffChange = Context.getInstance().getContextParams().getAverageBlockTime() * Context.getInstance().getContextParams().getDifficultyAdjustmentThreshold();
        long averageNetworkTime = latest.getTimestamp() - earliest.getTimestamp();

        return generateTargetBits(averageNetworkTime, timePerDiffChange, latest.getBits());
    }

    private static int generateTargetBits(long actualTimespan, long timeRequired, int prevTarget) {
        BigInteger target = targetIntegerFromBits(prevTarget);

        if (actualTimespan < (timeRequired / 4)) {
            actualTimespan = (timeRequired / 4);
        }

        if (actualTimespan > (timeRequired * 4)) {
            actualTimespan = (timeRequired * 4);
        }

        target = setCompact(target).multiply(BigInteger.valueOf(actualTimespan))
                .divide(BigInteger.valueOf(timeRequired));

        target = target.min(Context.getInstance().getContextParams().getMaxTarget());

        return getCompact(target);
    }

    protected static final BigInteger setCompact(BigInteger integer) {
        return targetIntegerFromBits(getCompact(integer));
    }

    protected static final int getCompact(BigInteger integer) {
        byte bytes[]    = Utils.takeApart(integer);
        byte bits[]     = new byte[4];

        bits[0] = (byte) bytes.length;
        if (bytes.length > 0)
            bits[1] = bytes[0];
        if (bytes.length > 1)
            bits[2] = bytes[1];
        if (bytes.length > 2)
            bits[3] = bytes[2];

        return Utils.makeInt(bits);
    }
}
