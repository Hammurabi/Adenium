package io.adenium.core.transactions;

import io.adenium.core.*;
import io.adenium.crypto.ec.RecoverableSignature;
import org.json.JSONObject;
import org.wolkenproject.core.*;
import io.adenium.core.events.DepositFundsEvent;
import io.adenium.core.events.WithdrawFundsEvent;
import io.adenium.crypto.Signature;
import io.adenium.encoders.Base16;
import io.adenium.encoders.Base58;
import io.adenium.exceptions.WolkenException;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.VarInt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class BasicTransaction extends Transaction {
    // must be a valid 20 byte address hash160(hash256(publicKey))
    private byte recipient[];
    // value of the transfer
    private long value;
    // maximum fee that sender is willing to pay
    private long fee;
    // transaction index
    private long nonce;
    // a recoverable ec signature
    private RecoverableSignature signature;

    public BasicTransaction() {
        this(new byte[Address.RawLength], 0, 0, 0);
    }

    public BasicTransaction(byte recipient[], long value, long fee, long nonce) {
        this.recipient  = recipient;
        this.value      = value;
        this.fee        = fee;
        this.nonce      = nonce;
        this.signature = new RecoverableSignature();
    }

    @Override
    public int getFlags() {
        return 0;
    }

    @Override
    public long getTransactionValue() {
        return value;
    }

    @Override
    public long getTransactionFee() {
        return fee;
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
    public Address getSender() throws WolkenException {
        return Address.fromKey(signature.recover(asByteArray()));
    }

    @Override
    public Address getRecipient() {
        return Address.fromRaw(recipient);
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
        return VarInt.sizeOfCompactUin32(getVersion(), false) + 20 +
                VarInt.sizeOfCompactUin64(value, false) +
                VarInt.sizeOfCompactUin64(fee, false) +
                VarInt.sizeOfCompactUin64(nonce, false) +
                65;
    }

    @Override
    public TransactionCode checkTransaction() {
        try {
            Account account = Context.getInstance().getDatabase().findAccount(getSender().getRaw());
            if (account == null) {
                return TransactionCode.InvalidTransaction;
            }

            boolean valid =
                            commonTransactionChecks(value, fee) &&
                            // check signature data is sound.
                            (signature.getR().length == 32) &&
                            (signature.getS().length == 32) &&
                            getSender() != null &&
                            // check the account account and balance of sender.
                            (account.getNonce()) < nonce &&
                            (account.getBalance()) >= (value + fee);

            if (!valid) {
                return TransactionCode.InvalidTransaction;
            }

            if (isFutureNonce(account.getNonce(), nonce)) {
                return TransactionCode.FutureTransaction;
            }

            return TransactionCode.ValidTransaction;
        } catch (WolkenException e) {
            return TransactionCode.InvalidTransaction;
        }
    }

    @Override
    public boolean verify(BlockStateChange stateChange, Block block, int blockHeight, long fees) {
        Address sender = null;
        try {
            sender = getSender();

            long balance = stateChange.getAccountBalance(sender.getRaw(), true, true);

            if (balance >= value + fees) {
                stateChange.createAccountIfDoesNotExist(recipient);
                stateChange.addEvent(new DepositFundsEvent(recipient, value));
                stateChange.addEvent(new WithdrawFundsEvent(sender.getRaw(), value + fee));
                return true;
            }
        } catch (WolkenException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void getStateChange(Block block, BlockStateChange stateChange) throws WolkenException {
        Address sender = getSender();
        stateChange.createAccountIfDoesNotExist(recipient);
        stateChange.addEvent(new DepositFundsEvent(recipient, value));
        stateChange.addEvent(new WithdrawFundsEvent(sender.getRaw(), value + fee));
    }

    @Override
    public JSONObject toJson(boolean txEvt, boolean evHash) {
        JSONObject txHeader = new JSONObject().put("name", getClass().getName()).put("version", getVersion());
        txHeader.put("content", new JSONObject()
                .put("recipient", Base58.encode(recipient)))
                .put("value", value)
                .put("fee", fee)
                .put("nonce", nonce)
                .put("v", signature.getV())
                .put("r", Base16.encode(signature.getR()))
                .put("s", Base16.encode(signature.getS()));
        return txHeader;
    }

    @Override
    protected void setSignature(Signature signature) throws WolkenException {
        if (signature instanceof RecoverableSignature) {
            this.signature = (RecoverableSignature) signature;
        }

        throw new WolkenException("invalid signature type '" + signature.getClass() + "'.");
    }

    @Override
    protected Transaction copyForSignature() {
        return new BasicTransaction(Arrays.copyOf(recipient, recipient.length),value,fee,nonce);
    }

    @Override
    public void write(OutputStream stream) throws IOException, WolkenException {
        stream.write(recipient);
        VarInt.writeCompactUInt64(value, false, stream);
        VarInt.writeCompactUInt64(fee, false, stream);
        VarInt.writeCompactUInt64(nonce, false, stream);
        signature.write(stream);
    }

    @Override
    public void read(InputStream stream) throws IOException, WolkenException {
        checkFullyRead(stream.read(recipient), 20);
        value   = VarInt.readCompactUInt64(false, stream);
        fee     = VarInt.readCompactUInt64(false, stream);
        nonce   = VarInt.readCompactUInt64(false, stream);
        signature.read(stream);
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws WolkenException {
        return (Type) new BasicTransaction();
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(BasicTransaction.class);
    }
}
