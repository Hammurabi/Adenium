package io.adenium.crypto;

import io.adenium.exceptions.WolkenException;

import java.math.BigInteger;

public abstract class Key {
    // return the actual key as an integer
    public abstract BigInteger asInteger();
    // should return only the significant key bytes
    public abstract byte[] getRaw();
    // should return a neatly packed version of this key
    public abstract byte[] getEncoded();
    // should return a compressed version of this key
    public abstract Key getCompressed() throws WolkenException;
    // should return a decompressed version of this key
    public abstract Key getDecompressed() throws WolkenException;
    // should return true only if the two keys match
    public abstract boolean equals(Key other);
}
