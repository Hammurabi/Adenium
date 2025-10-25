package io.adenium.crypto.ec;

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import io.adenium.crypto.CryptoLib;
import io.adenium.crypto.CryptoUtil;
import io.adenium.crypto.Key;
import io.adenium.exceptions.AdeniumException;
import io.adenium.utils.HashUtil;
import io.adenium.utils.Utils;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.util.Arrays;

public class ECPrivateKey extends Key {
    private final byte key[];

    public ECPrivateKey() {
        this(generateValidKey());
    }

    public ECPrivateKey(byte key[]) {
        this.key = key;
    }

    @Override
    public BigInteger asInteger() {
        return new BigInteger(1, key);
    }

    @Override
    public byte[] getRaw() {
        return Utils.trim(key, 1, key.length - 1);
    }

    @Override
    public byte[] getEncoded() {
        return key;
    }

    @Override
    public Key getCompressed() throws AdeniumException {
        return this;
    }

    @Override
    public Key getDecompressed() throws AdeniumException {
        return this;
    }

    @Override
    public boolean equals(Key other) {
        return Arrays.equals(getEncoded(), other.getEncoded());
    }

    public static byte[] generateValidKey() {
        byte bytes[] = HashUtil.sha256(CryptoUtil.makeSecureBytes(1024));

        try {
            ECParameterSpec ecParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1");

            ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(1, bytes), ecParameterSpec);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            BCECPrivateKey key = (BCECPrivateKey) keyFactory.generatePrivate(privateKeySpec);

            while (key.getD().bitLength() > CryptoLib.getCurve().getN().bitLength()) {
                bytes = HashUtil.sha256(CryptoUtil.makeSecureBytes(1024));

                privateKeySpec = new ECPrivateKeySpec(new BigInteger(1, bytes), ecParameterSpec);
                keyFactory = KeyFactory.getInstance("EC");
                key = (BCECPrivateKey) keyFactory.generatePrivate(privateKeySpec);
            }
        } catch (Exception e) {
        }

        return bytes;
    }
}
