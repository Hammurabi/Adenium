package io.adenium.core.assets;

import io.adenium.core.Account;
import io.adenium.core.Context;

import java.math.BigInteger;

public abstract class Token extends Asset {
    // 256bit unsigned integer representing the total supply of this asset.
    private final BigInteger totalSupply;

    public Token() {
        this(DefaultUUID, BigInteger.ZERO);
    }

    public Token(byte[] uuid, BigInteger totalSupply) {
        super(uuid);
        this.totalSupply = totalSupply;
    }

    public abstract void issue(BigInteger amount, byte receivingAddress[]);
    public abstract void transfer(byte sender[], byte receiver[], BigInteger amount);

    // deposit 'amount' into 'address'.
    public abstract void deposit(byte address[], BigInteger amount);

    // return true if the 'address' has at least '1' of this token.
    public boolean hasToken(byte address[]) {
        Account account = Context.getInstance().getDatabase().findAccount(address);
        if (account != null) {
            return account.getBalanceForToken(getUUID()).compareTo(BigInteger.ZERO) > 0;
        }

        return false;
    }

    // return the amount of this token owned by 'address'.
    public BigInteger balanceOf(byte address[]) {
        Account account = Context.getInstance().getDatabase().findAccount(address);
        if (account != null) {
            return account.getBalanceForToken(getUUID());
        }

        return BigInteger.ZERO;
    }

    public boolean isTransferable() {
        return false;
    }

    @Override
    public boolean isFungible() {
        return false;
    }

    @Override
    public BigInteger getTotalSupply() {
        return null;
    }
}
