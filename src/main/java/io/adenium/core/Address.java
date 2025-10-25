package io.adenium.core;

import io.adenium.crypto.Key;
import io.adenium.encoders.Base58;
import io.adenium.utils.HashUtil;
import io.adenium.utils.Utils;

public class Address {
    public static final int RawLength = 20;
    private int     prefix;
    private byte[]  raw;
    private byte[]  checksum;

    private Address(byte raw[]) {
        this(Context.getInstance().getContextParams().getGenericAddressPrefix(), raw);
    }

    private Address(byte prefix, byte raw[]) {
        this(prefix, raw, Utils.trim(HashUtil.sha256d(Utils.concatenate(new byte[] { prefix }, raw)), 0, 4));
    }

    private Address(byte prefix, byte raw[], byte checksum[]) {
        this.prefix     = Byte.toUnsignedInt(prefix);
        this.raw        = raw;
        this.checksum   = checksum;
    }
    public static Address fromKey(Key key) {
        return fromKey(Context.getInstance().getContextParams().getGenericAddressPrefix(), key);
    }

    // return an address object from a key
    public static Address fromKey(int prefix, Key key) {
        if (key == null) {
            return null;
        }

        // get the encoded key (with prefix byte)
        byte encodedKey[]   = key.getEncoded();

        // return a hash160 of the key (with a network 'address' prefix)
        return new Address((byte) prefix, HashUtil.hash160(encodedKey));
    }


    // return an address object from a 'raw' non-encoded 20 byte address
    public static Address fromRaw(byte[] rawBytes) {
        return new Address(rawBytes);
    }

    public static Address newContractAddress(byte[] sender, long nonce) {
        return new Address(HashUtil.hash160(Utils.concatenate(sender, Utils.takeApartLong(nonce))));
    }

    public static Address fromFormatted(byte[] formattedAddress) {
        return new Address(formattedAddress[0], Utils.trim(formattedAddress, 1, 20), Utils.trim(formattedAddress, 21, 4));
    }

    public static boolean isValidAddress(byte[] address) {
        if (address.length != 25) {
            return false;
        }

        byte prefixed[] = Utils.trim(address, 0, 21);

        if (prefixed[0] != Context.getInstance().getContextParams().getGenericMainnetAddressPrefix() && prefixed[0] != Context.getInstance().getContextParams().getGenericTestnetAddressPrefix()) {
            return false;
        }

        return Utils.equals(Utils.trim(HashUtil.sha256d(prefixed), 0, 4), Utils.trim(address, 21, 4));
    }

    public byte[] getRaw() {
        return raw;
    }

    @Override
    public String toString() {
        return Base58.encode(getFormatted());
    }

    public byte[] getFormatted() {
        return Utils.concatenate(new byte[] {(byte) prefix}, raw, checksum);
    }
}
