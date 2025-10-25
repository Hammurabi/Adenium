package io.adenium.core.transactions;

import io.adenium.core.Address;
import io.adenium.core.Block;
import io.adenium.core.BlockStateChange;
import io.adenium.core.Context;
import org.json.JSONObject;
import io.adenium.core.events.MintRewardEvent;
import io.adenium.crypto.Signature;
import io.adenium.encoders.Base16;
import io.adenium.encoders.Base58;
import io.adenium.exceptions.AdeniumException;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.ChainMath;
import io.adenium.utils.VarInt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class MintTransaction extends Transaction {
    // the amount of coins to be minted
    private long value;
    // the recipient
    private byte recipient[];
    // a small data-dump for the miner
    // can be used as an extra nonce
    private byte dump[];

    protected MintTransaction() {
        this(0, new byte[Address.RawLength], new byte[0]);
    }

    protected MintTransaction(long value, byte recipient[], byte dump[]) {
        this.value      = value;
        this.recipient  = recipient;
        this.dump       = dump;
    }

    public void setFees(long fees) {
        this.value += fees;
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
        return 0;
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
        return dump.length <= 1024 ? TransactionCode.ValidTransaction : TransactionCode.InvalidTransaction;
    }

    @Override
    public Address getSender() throws AdeniumException {
        return null;
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
        return VarInt.sizeOfCompactUin32(getVersion(), false) + 20 + VarInt.sizeOfCompactUin32(dump.length, false) + dump.length;
    }

    @Override
    public boolean verify(BlockStateChange blockStateChange, Block block, int blockHeight, long fees) {
        return value == ( ChainMath.getReward(blockHeight) + block.getFees() );
    }

    @Override
    public void getStateChange(Block block, BlockStateChange stateChange) {
        stateChange.createAccountIfDoesNotExist(recipient);
        stateChange.addEvent(new MintRewardEvent(recipient, value));
    }

    @Override
    public JSONObject toJson(boolean txEvt, boolean evHash) {
        JSONObject txHeader = new JSONObject().put("name", getClass().getName()).put("version", getVersion());
        txHeader.put("content", new JSONObject().put("value", value).put("recipient", Base58.encode(recipient))).put("dump", Base16.encode(dump));
        return txHeader;
    }

    @Override
    protected void setSignature(Signature signature) {
    }

    @Override
    protected Transaction copyForSignature() {
        return new MintTransaction(value, Arrays.copyOf(recipient, recipient.length), Arrays.copyOf(dump, dump.length));
    }

    @Override
    public void write(OutputStream stream) throws IOException, AdeniumException {
        VarInt.writeCompactUInt64(value, false, stream);
        stream.write(recipient);
        VarInt.writeCompactUInt32(dump.length, false, stream);
        if (dump.length > 0) {
            stream.write(dump);
        }
    }

    @Override
    public void read(InputStream stream) throws IOException, AdeniumException {
        value = VarInt.readCompactUInt64(false, stream);
        checkFullyRead(stream.read(recipient), Address.RawLength);
        int length = VarInt.readCompactUInt32(false, stream);
        if (length > 0) {
            dump = new byte[length];
            checkFullyRead(stream.read(dump), length);
        } else {
            dump = new byte[0];
        }
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
        return (Type) new MintTransaction();
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(MintTransaction.class);
    }
}
