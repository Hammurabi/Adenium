package io.adenium.utils;

public abstract class Hashable {
    private byte[] hash160;
    private byte[] hash256;
    private byte[] hash256d;

    abstract byte[] getHash160();
    abstract byte[] getHash256();
    abstract byte[] getHash256d();
}
