package io.adenium.core;

import io.adenium.exceptions.AdeniumException;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.VarInt;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

public class Account extends SerializableI {
    /* used for transaction indexing */
    private long    nonce;
    /* the current balance of this account */
    private long    balance;
    /* true if the account has an alias */
    private boolean hasAlias;
    /* the account alias */
    private long    alias;

    public Account() {
        this(0, 0, false, 0);
    }

    public Account(long nonce, long balance, boolean hasAlias, long alias) {
        this.nonce      = nonce;
        this.balance    = balance;
        this.hasAlias   = hasAlias;
        this.alias      = alias;
    }

    @Override
    public void write(OutputStream stream) throws IOException, AdeniumException {
        // in the case of nonce 2^61-1 should suffice for now
        VarInt.writeCompactUInt64(nonce, false, stream);
        // as for balance, coincidentally/fortunately, the maximum
        // number of coins to be issues matches 61 bits exactly
        // and so for now, it's safe to drop the last 3 bits.
        VarInt.writeCompactUInt64(balance, false, stream);

        stream.write(hasAlias ? 1 : 0);

        if (hasAlias) {
            VarInt.writeCompactUInt64(alias, false, stream);
        }
    }

    @Override
    public void read(InputStream stream) throws IOException, AdeniumException {
        nonce   = VarInt.readCompactUInt64(false, stream);
        balance = VarInt.readCompactUInt64(false, stream);

        hasAlias = checkNotEOF(stream.read()) == 1;

        if (hasAlias) {
            alias = VarInt.readCompactUInt64(false, stream);
        }
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
        return (Type) new Account(0, 0, false, 0);
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(Account.class);
    }

    public long getNonce() {
        return nonce;
    }

    public long getBalance() {
        return balance;
    }

    public boolean hasAlias() {
        return hasAlias;
    }

    public long getAlias() {
        return alias;
    }

    public Account updateBalance(long balance) {
        return new Account(nonce, balance, hasAlias, alias);
    }

    public Account withdraw(long amount) {
        return new Account(nonce, balance - amount, hasAlias, alias);
    }

    public Account undoWithdraw(long amount) {
        return new Account(nonce, balance + amount, hasAlias, alias);
    }

    public Account undoDeposit(long amount) {
        return new Account(nonce, balance - amount, hasAlias, alias);
    }

    public Account deposit(long amount) {
        return new Account(nonce, balance + amount, hasAlias, alias);
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject().put("nonce", nonce).put("balance", balance);
        if (hasAlias) {
            json.put("alias", alias);
        }

        return json;
    }

    public BigInteger getBalanceForToken(byte[] uuid) {
        return BigInteger.ZERO;
    }
}
