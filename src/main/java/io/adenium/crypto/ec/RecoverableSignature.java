package io.adenium.crypto.ec;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.math.ec.ECPoint;
import io.adenium.core.Context;
import io.adenium.crypto.CryptoLib;
import io.adenium.crypto.Key;
import io.adenium.crypto.Signature;
import io.adenium.exceptions.AdeniumException;
import io.adenium.serialization.SerializableI;
import io.adenium.utils.Assertions;
import io.adenium.utils.HashUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;

public class RecoverableSignature extends Signature {
    private final byte v[];
    private final byte r[];
    private final byte s[];

    public RecoverableSignature() {
        this((byte) 0, new byte[32], new byte[32]);
    }

    public RecoverableSignature(int v, byte[] r, byte[] s) {
        this.v = new byte[] {(byte) v};
        this.r = r;
        this.s = s;
    }

    public byte getV() {
        return v[0];
    }

    public byte[] getR() {
        return r;
    }

    public byte[] getS() {
        return s;
    }

    @Override
    public void write(OutputStream stream) throws IOException, AdeniumException {
        stream.write(v);
        stream.write(r);
        stream.write(s);
    }

    @Override
    public void read(InputStream stream) throws IOException, AdeniumException {
        checkFullyRead(stream.read(v), 1);
        checkFullyRead(stream.read(r), 32);
        checkFullyRead(stream.read(s), 32);
    }

    @Override
    public <Type extends SerializableI> Type newInstance(Object... object) throws AdeniumException {
        return (Type) new RecoverableSignature();
    }

    @Override
    public int getSerialNumber() {
        return Context.getInstance().getSerialFactory().getSerialNumber(RecoverableSignature.class);
    }

    @Override
    public boolean checkSignature(byte[] originalMessage, Key publicKey) {
        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));

        X9ECParameters params = SECNamedCurves.getByName("secp256k1");
        ECDomainParameters curve = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());

        ECPoint point = curve.getCurve().decodePoint(publicKey.getEncoded());

        signer.init(false, new ECPublicKeyParameters(point, CryptoLib.getCurve()));
        return signer.verifySignature(HashUtil.sha256d(originalMessage), new BigInteger(1, r), new BigInteger(1, s));
    }

    @Override
    public Key recover(byte originalMessage[]) throws AdeniumException {
        Assertions.assertTrue(r != null && r.length == 32, "r must be 32 bytes in length");
        Assertions.assertTrue(s != null && s.length == 32, "s must be 32 bytes in length");

        int header = Byte.toUnsignedInt(v[0]) & 0xFF;

        if (header < 53 || header > 128) {
            throw new AdeniumException("header byte out of range: " + header);
        }

        ECSig sig = new ECSig(new BigInteger(1, r), new BigInteger(1, s));
        Key result = CryptoLib.recoverFromSignature(v[0] - 53, sig, HashUtil.sha256d(originalMessage));

        if (result == null) {
            throw new AdeniumException("could not recover public key.");
        }

        return result;
    }
}