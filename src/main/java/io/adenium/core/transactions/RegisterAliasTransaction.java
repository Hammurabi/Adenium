package io.adenium.core.transactions;

import io.adenium.core.*;
import io.adenium.crypto.ec.RecoverableSignature;
import org.json.JSONObject;
import io.adenium.core.events.RegisterAliasEvent;
import io.adenium.crypto.Signature;
import io.adenium.encoders.Base16;
import io.adenium.exceptions.AdeniumException;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.VarInt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RegisterAliasTransaction extends Transaction {
    // nonce
    private long nonce;
    // fee to register the alia
    private long fee;
    // alias
    private long alias;
    // signature of the sender
    private RecoverableSignature signature;

    protected RegisterAliasTransaction() {
        this(0, 0, 0);
    }

    protected RegisterAliasTransaction(long nonce, long fee, long alias) {
        this.nonce  = nonce;
        this.fee    = fee;
        this.alias  = alias;
        this.signature = new RecoverableSignature();
    }

    @Override
    public int getFlags() {
        return 0;
    }

    @Override
    public long getTransactionValue() {
        return 0;
    }

    @Override
    public long getTransactionFee() {
        return fee + Context.getInstance().getContextParams().getAliasRegistrationCost();
    }

    @Override
    public long getMaxUnitCost() {
        return 0;
    }

    @Override
    public byte[] getPayload() {
        return new byte[0];
    }

    @Override
    public TransactionCode checkTransaction() {
        // this is not 100% necessary
        // a transfer of 0 with a fee of 0 is not allowed
        try {
            Account account = Context.getInstance().getDatabase().findAccount(getSender().getRaw());
            if (account == null) {
                return TransactionCode.InvalidTransaction;
            }

            boolean valid =
                    fee > 0 &&
                    account.getNonce() < nonce &&
                    !account.hasAlias() &&
                    !Context.getInstance().getDatabase().checkAccountExists(alias) &&
                    (signature.getR().length == 32) &&
                    (signature.getS().length == 32) &&
                    getSender() != null;

            if (!valid) {
                return TransactionCode.InvalidTransaction;
            }

            if (isFutureNonce(account.getNonce(), nonce)) {
                return TransactionCode.FutureTransaction;
            }

            return TransactionCode.ValidTransaction;
        } catch (AdeniumException e) {
        }

        return TransactionCode.InvalidTransaction;
    }

    @Override
    public Address getSender() throws AdeniumException {
        return Address.fromKey(signature.recover(asByteArray()));
    }

    @Override
    public Address getRecipient() {
        return null;
    }

    @Override
    public boolean hasMultipleSenders() {
        return false;
    }

    @Override
    public boolean hasMultipleRecipients() {
        return false;
    }

    @Override
    public int calculateSize() {
        return
                VarInt.sizeOfCompactUin32(getVersion(), false) +
                VarInt.sizeOfCompactUin64(nonce, false) +
                VarInt.sizeOfCompactUin64(alias, false) + 65;
    }

    @Override
    public boolean verify(BlockStateChange blockStateChange, Block block, int blockHeight, long fees) {
        return false;
    }

    @Override
    public void getStateChange(Block block, BlockStateChange stateChange) throws AdeniumException {
        Address sender = getSender();
        stateChange.createAccountIfDoesNotExist(sender.getRaw());
        stateChange.addEvent(new RegisterAliasEvent(sender.getRaw(), alias));
    }

    @Override
    public JSONObject toJson(boolean txEvt, boolean evHash) {
        JSONObject txHeader = new JSONObject().put("name", getClass().getName()).put("version", getVersion());
        txHeader.put("content", new JSONObject().put("nonce", nonce).put("fee", fee).put("alias", alias).put("v", signature.getV()).put("r", Base16.encode(signature.getR())).put("s", Base16.encode(signature.getS())));
        return txHeader;
    }

    @Override
    protected void setSignature(Signature signature) throws AdeniumException {
        if (signature instanceof RecoverableSignature) {
            this.signature = (RecoverableSignature) signature;
        }

        throw new AdeniumException("invalid signature type '" + signature.getClass() + "'.");
    }

    @Override
    protected Transaction copyForSignature() {
        return new RegisterAliasTransaction(nonce, fee, alias);
    }

    @Override
    public void write(OutputStream stream) throws IOException, AdeniumException {
        signature.write(stream);
        VarInt.writeCompactUInt64(alias, false, stream);
    }

    @Override
    public void read(InputStream stream) throws IOException, AdeniumException {
        signature.read(stream);
        alias = VarInt.readCompactUInt64(false, stream);
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
        return (Type) new RegisterAliasTransaction();
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(MintTransaction.class);
    }
}