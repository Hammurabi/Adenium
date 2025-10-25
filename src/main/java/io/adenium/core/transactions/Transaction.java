package io.adenium.core.transactions;

import io.adenium.core.*;
import io.adenium.crypto.ec.RecoverableSignature;
import org.json.JSONObject;
import io.adenium.crypto.Keypair;
import io.adenium.crypto.Signature;
import io.adenium.encoders.Base16;
import io.adenium.encoders.Base58;
import io.adenium.exceptions.AdeniumException;
import io.adenium.serialization.SerializableI;
import io.adenium.serialization.SerializationFactory;
import io.adenium.utils.HashUtil;
import io.adenium.utils.VarInt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Transaction extends SerializableI implements Comparable<Transaction> {
    public static int UniqueIdentifierLength = 32;

    public enum TransactionCode {
        InvalidTransaction,
        FutureTransaction,
        ValidTransaction,
    }

    public static Transaction fromJson(JSONObject transaction) throws AdeniumException {
        if (transaction != null) {
            // check for the header information
            if (transaction.has("name") && transaction.has("version")) {
                String name = transaction.getString("name");
                int version = transaction.getInt("version");

                if (name.equals("BasicTransaction")) {
                    JSONObject content  = transaction.getJSONObject("content");
                    String recipient    = content.getString("recipient");
                    long value          = content.getLong("value");
                    long fee            = content.getLong("fee");
                    long nonce          = content.getLong("nonce");

                    byte recipientBytes[] = null;

                    if (Base16.isEncoded(recipient)) {
                        recipientBytes  = Base16.decode(recipient);
                    } else if (Base58.isEncoded(recipient)) {
                        recipientBytes  = Base58.decode(recipient);
                    } else {
                        throw new AdeniumException("'recipient' expected to be base58 encoded.");
                    }

                    Address address = null;

                    if (recipientBytes.length != 20) {
                        if (!Address.isValidAddress(recipientBytes)) {
                            throw new AdeniumException("'recipient' expected to be a valid address.");
                        } else {
                            address = Address.fromFormatted(recipientBytes);
                        }
                    } else {
                        address = Address.fromRaw(recipientBytes);
                    }

                    Transaction txn = newTransfer(address, value, fee, nonce);

                    // has signature
                    if (content.has("v") && content.has("r") && content.has("s")) {
                        int v               = content.getInt("v");
                        String r            = content.getString("r");
                        String s            = content.getString("s");

                        RecoverableSignature signature = new RecoverableSignature(v, Base16.decode(r), Base16.decode(s));
                        txn.setSignature(signature);
                    }

                    return txn;
                } else if (name.equals("RegisterAliasTransaction")) {
                    JSONObject content  = transaction.getJSONObject("content");
                    long nonce          = content.getLong("nonce");
                    long fee            = content.getLong("fee");
                    long alias          = content.getLong("alias");

                    Transaction txn = newAliasRegistration(nonce, fee, alias);

                    // has signature
                    if (content.has("v") && content.has("r") && content.has("s")) {
                        int v               = content.getInt("v");
                        String r            = content.getString("r");
                        String s            = content.getString("s");

                        RecoverableSignature signature = new RecoverableSignature(v, Base16.decode(r), Base16.decode(s));
                        txn.setSignature(signature);
                    }

                    return txn;
                }
            }
        }

        throw new AdeniumException("could not convert to a transaction.");
    }

    public static final class Flags
    {
        public static final int
                NoFlags             = 0,
                TwoByteFlags        = 1,
                MochaPayload        = 1<<1,
                MultipleRecipients  = 1<<2,
                UseAliases          = 1<<3,
                UnusedFlag1         = 1<<4,
                UnusedFlag2         = 1<<5,
                UnusedFlag3         = 1<<5,
                UnusedFlag4         = 1<<6,
                UnusedFlag5         = 1<<7,
                UnusedFlag6         = 1<<8
        ;
    }

    // this is not serialized
    private byte txid[];

    // can be represented by 1 - 4 bytes
    // version = 1 skips flags all-together

    // this should not be treated as a network version
    // transaction versions should be treated as VARINT
    // magic numbers that hint at the internal transaction
    // structure.
    // anything below here is optional
    public int getVersion() {
        return getSerialNumber();
    }

    public abstract int getFlags();
    public abstract long getTransactionValue();
    public abstract long getTransactionFee();
    public abstract long getMaxUnitCost();
    public abstract byte[] getPayload();
    /*
        shallow checks of the validity of a transactions
        check the receiver is valid
        check the sender is valid
        check the signature is valid
        check the sender has funds
     */
    public abstract TransactionCode checkTransaction();
    public abstract Address getSender() throws AdeniumException;
    public abstract Address getRecipient();
    public abstract boolean hasMultipleSenders();
    public abstract boolean hasMultipleRecipients();
    public abstract int calculateSize();

    public static final boolean commonTransactionChecks(long transferAmount, long transferFee) {
        if (transferAmount <= 0) return false;
        if (transferFee <= 0) return false;
//        //possible vulnerability with a+b!=0 using signed integers
//        if (getTransactionValue() + getTransactionFee()) > 0

        return true;
    }

    public static final boolean isFutureNonce(long accountNonce, long transactionNonce) {
        return (accountNonce + 1) < transactionNonce;
    }

    /*
            deep checks of the validity of a transactions
            if the transaction contains a payload, the pa
            -yload would be executed and if errors are th
            -rown without being caught then the transacti
            -on is deemed invalid.
         */
    public abstract boolean verify(BlockStateChange blockStateChange, Block block, int blockHeight, long fees);
    /*
        return all the changes this transaction will
        cause to the global state.
     */
    public abstract void getStateChange(Block block, BlockStateChange stateChange) throws AdeniumException;

    public JSONObject toJson() {
        return toJson(true, false);
    }

    public abstract JSONObject toJson(boolean txEvt, boolean evHash);

    public Transaction sign(Keypair keypair) throws AdeniumException {
        // this includes the version bytes
        byte tx[] = asSerializedArray();
        Signature signature = keypair.sign(tx);
        Transaction transaction = copyForSignature();
        transaction.setSignature(signature);

        return transaction;
    }

    @Override
    public String toString() {
        return Base16.encode(getHash());
    }

    protected abstract void setSignature(Signature signature) throws AdeniumException;

    protected abstract Transaction copyForSignature();

    // multiple recipients and senders might be possible in the future
    public Address[] getSenders() throws AdeniumException {
        return new Address[] { getSender() };
    }
    public Address[] getRecipients() {
        return new Address[] { getRecipient() };
    }

    public byte[] getHash() {
        if (txid == null) {
            txid = HashUtil.sha256d(asByteArray());
        }

        return txid;
    }

    @Override
    public int compareTo(Transaction transaction) {
        return (getMaxUnitCost() > transaction.getMaxUnitCost() ? 1 : -1);
    }

    // a transaction that creates a contract.
    public static Transaction newPayload(long amount, long fee, long nonce, byte payLoad[]) {
        return new PayloadTransaction(amount, fee, nonce, payLoad);
    }

    // a purely monetary transaction
    public static Transaction newTransfer(Address recipient, long amount, long fee, long nonce) {
        return new BasicTransaction(recipient.getRaw(), amount, fee, nonce);
    }

    // a purely monetary transaction
    public static Transaction newAliasRegistration(long nonce, long fee, long alias) {
        return new RegisterAliasTransaction(nonce, fee, alias);
    }

    public static Transaction newMintTransaction(String msg, long reward, Address addresses) {
        return new MintTransaction(reward, addresses.getRaw(), msg.getBytes());
    }

    public static final void register(SerializationFactory factory) {
        factory.registerClass(MintTransaction.class, new MintTransaction());
        factory.registerClass(BasicTransaction.class, new BasicTransaction());
        factory.registerClass(BasicTransactionToAlias.class, new BasicTransactionToAlias());
        factory.registerClass(RegisterAliasTransaction.class, new RegisterAliasTransaction());
        factory.registerClass(PayloadTransaction.class, new PayloadTransaction());
    }

    // this is a basic payload transaction (contract creation)
    // transfer value is sent to the contract's account
    // min size: 1 + 68 + (varint) + payload
    // avg size: 1 + 79 + (varint) + payload
    // max size: 1 + 89 + (varint) + payload
    public static final class PayloadTransaction extends Transaction {
        // not serialized
        private List<Event> stateChangeEvents;
        // value of the transfer
        private long value;
        // maximum amount that sender is willing to pay
        private long fee;
        // maximum fee that sender is willing to pay
        private long unitCost;
        // transaction index
        private long nonce;
        // a recoverable ec signature
        private RecoverableSignature signature;
        // a valid mocha payload
        private byte payload[];

        public PayloadTransaction() {
            this(0, 0, 0, new byte[0]);
        }

        public PayloadTransaction(long value, long fee, long nonce, byte payload[]) {
            this.value  = value;
            this.fee    = fee;
            this.nonce  = nonce;
            this.payload= payload;
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
            return unitCost;
        }

        @Override
        public byte[] getPayload() {
            return new byte[0];
        }

        @Override
        public TransactionCode checkTransaction() {
            return TransactionCode.InvalidTransaction;
        }

        @Override
        public Address getSender() throws AdeniumException {
            return Address.fromKey(signature.recover(asByteArray()));
        }

        @Override
        public Address getRecipient() {
            try {
                return Address.newContractAddress(getSender().getRaw(), nonce);
            } catch (AdeniumException e) {
                return null;
            }
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
            return VarInt.sizeOfCompactUin32(getVersion(), false) +
                    VarInt.sizeOfCompactUin64(value, false) +
                    VarInt.sizeOfCompactUin64(unitCost, false) +
                    VarInt.sizeOfCompactUin64(fee, false) +
                    VarInt.sizeOfCompactUin64(nonce, false) +
                    VarInt.sizeOfCompactUin64(payload.length, false) +
                    payload.length +
                    65;
        }

        @Override
        public boolean verify(BlockStateChange blockStateChange, Block block, int blockHeight, long fees) {
            return false;
        }

        @Override
        public void getStateChange(Block block, BlockStateChange stateChange) throws AdeniumException {
            if (stateChangeEvents == null) {
                stateChangeEvents = new ArrayList<>();
            }

            stateChange.addEvents(stateChangeEvents);
        }

        @Override
        public JSONObject toJson(boolean txEvt, boolean evHash) {
            return null;
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
            return new PayloadTransaction(value, fee, nonce, Arrays.copyOf(payload, payload.length));
        }

        @Override
        public void write(OutputStream stream) throws IOException, AdeniumException {
            VarInt.writeCompactUInt64(value, false, stream);
            VarInt.writeCompactUInt64(fee, false, stream);
            VarInt.writeCompactUInt64(nonce, false, stream);
            signature.write(stream);
            VarInt.writeCompactUInt32(payload.length, false, stream);
            stream.write(payload);
        }

        @Override
        public void read(InputStream stream) throws IOException, AdeniumException {
            value   = VarInt.readCompactUInt64(false, stream);
            fee     = VarInt.readCompactUInt64(false, stream);
            nonce   = VarInt.readCompactUInt64(false, stream);
            signature.read(stream);
            int length = VarInt.readCompactUInt32(false, stream);
            if (length > 0) {
                payload = new byte[length];
                checkFullyRead(stream.read(payload), length);
            }
        }

        @Override
        public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
            return (Type) new PayloadTransaction();
        }

        @Override
        public int getSerialNumber() {
            return Context.getInstance().getSerialFactory().getSerialNumber(PayloadTransaction.class);
        }
    }

    // this is a modular transaction
    // it should be possible to use
    // flags to enable/disable specific
    // functionality
    public static final class FlaggedTransaction extends Transaction {
        // can be represented by 1 or more bytes
        // there are not enough flags at the moment
        // therefore it's represented by an int in
        // this version.
        private int flags;

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
            return TransactionCode.InvalidTransaction;
        }

        @Override
        public Address getSender() {
            return null;
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
            return 0;
        }

        @Override
        public boolean verify(BlockStateChange blockStateChange, Block block, int blockHeight, long fees) {
            return false;
        }

        @Override
        public void getStateChange(Block block, BlockStateChange stateChange) throws AdeniumException {
        }

        @Override
        public JSONObject toJson(boolean txEvt, boolean evHash) {
            return null;
        }

        @Override
        protected void setSignature(Signature signature) throws AdeniumException {
        }

        @Override
        protected Transaction copyForSignature() {
            return null;
        }

        @Override
        public void write(OutputStream stream) throws IOException, AdeniumException {
        }

        @Override
        public void read(InputStream stream) throws IOException, AdeniumException {
        }

        @Override
        public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
            return null;
        }

        @Override
        public int getSerialNumber() {
            return -1;
        }
    }
}
