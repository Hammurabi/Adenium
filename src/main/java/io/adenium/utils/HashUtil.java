package io.adenium.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

public class HashUtil {
    public static byte[] sha256(byte data[])
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return hash;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static byte[] sha1(byte data[])
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(data);
            return hash;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static byte[] sha3(byte data[])
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256", "BC");
            byte[] hash = digest.digest(data);
            return hash;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] sha512(byte data[]) {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-512", "BC");
            byte[] hash = digest.digest(data);
            return hash;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static byte[] sha256d(byte data[])
    {
        return sha256(sha256(data));
    }

    public static byte[] sha3d(byte data[])
    {
        return sha3(sha3(data));
    }

    public static byte[] sha512d(byte data[])
    {
        return sha512(sha512(data));
    }

    public static byte[] hmacsha512(byte[] key, byte[] data)
    {
        try
        {
            SHA512Digest digest = new SHA512Digest();
            HMac hMac = new HMac(digest);
            hMac.init(new KeyParameter(key));

            hMac.reset();
            hMac.update(data, 0, data.length);
            byte[] out = new byte[64];
            hMac.doFinal(out, 0);
            return out;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static byte[] hash160(byte data[]) {
        return ripemd160(sha256(data));
    }

    public static byte[] ripemd160(byte data[])
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("RIPEMD160", "BC");
            byte[] hash = digest.digest(data);
            return hash;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static byte[] reduceBits(byte msg[]) {
        final byte alphabet[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_".getBytes();
        Map<Byte, Integer> mapping = new HashMap<>();
        mapping.put((byte) 0, mapping.size());
        for (byte b : alphabet) {
            mapping.put(b, mapping.size());
        }

        BitOutputStream stream = new BitOutputStream();
        try {
            for (int i = 0; i < msg.length; i ++) {
                    stream.write(mapping.get(msg[i]), 6);
            }
        } catch (IOException e) {
            return null;
        }

        return stream.toByteArray();
    }

    // returns a hash of any string 'A-z|0-1'
    // will not have any collisions
    // finds the index of a given string in
    // n-dimensional space.
    public static byte[] hammurabi(byte msg[]) {
        final byte alphabet[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_".getBytes();
        Map<Byte, Integer> mapping = new HashMap<>();
        mapping.put((byte) 0, mapping.size());
        for (byte b : alphabet) {
            mapping.put(b, mapping.size());
        }

        final int blockSize = 5;
        final int singleDim = alphabet.length;
        final int numBlocks = (msg.length / blockSize + 1);
        final int paddedLen = numBlocks * blockSize;

        final byte padded[] = Arrays.copyOf(msg, paddedLen);
        final int blocks[]  = new int[numBlocks];

        for (int i = 0; i < padded.length; i += blockSize) {
            // prepare the block
            int indexOf0 = mapping.get(padded[i]);
            int indexOf1 = mapping.get(padded[i + 1]);
            int indexOf2 = mapping.get(padded[i + 2]);
            int indexOf3 = mapping.get(padded[i + 3]);
            int indexOf4 = mapping.get(padded[i + 4]);

            // map 5d to 1d
            blocks[i / blockSize] =
                    singleDim*singleDim*singleDim*singleDim * indexOf0
                  + singleDim*singleDim*singleDim           * indexOf1
                  + singleDim*singleDim                     * indexOf2
                  + singleDim                               * indexOf3
                  +                                           indexOf4
                    ;
        }

        // calculate compact hash
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (int i = 0; i < numBlocks; i ++) {
            try {
                // 2 bits of precision is lost but since block dimension size is 63^5 30 bits should suffice.
                VarInt.writeCompactUInt32(blocks[i], false, stream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return stream.toByteArray();
    }
}
