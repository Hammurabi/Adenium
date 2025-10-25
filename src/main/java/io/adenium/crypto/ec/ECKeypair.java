package io.adenium.crypto.ec;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import io.adenium.crypto.CryptoLib;
import io.adenium.crypto.Key;
import io.adenium.crypto.Keypair;
import io.adenium.crypto.Signature;
import io.adenium.exceptions.AdeniumException;
import io.adenium.utils.HashUtil;
import io.adenium.utils.Utils;

import java.math.BigInteger;
import java.util.Arrays;

public class ECKeypair extends Keypair {
    public ECKeypair(Key priv) throws AdeniumException {
        this(priv, publicKeyFromPrivate(priv.asInteger()));
    }

    public ECKeypair(Key priv, Key pubk) {
        super(priv, pubk);
    }

    public ECSig ellipticCurveSign(byte message[]) {
        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));

        ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(getPrivateKey().asInteger(), CryptoLib.getCurve());
        signer.init(true, privateKeyParameters);

        BigInteger components[] = signer.generateSignature(message);

        return new ECSig(components[0], components[1]).toCanonicalised();
    }

    @Override
    public Signature sign(byte[] message) throws AdeniumException {
        BigInteger publicKey = getPublicKey().asInteger();
        // hash with sha256d
        byte[] messageHash = HashUtil.sha256d(message);

        ECSig sig = ellipticCurveSign(messageHash);

        // Now we have to work backwards to figure out the recId needed to recover the signature.
        int recId = -1;
        for (int i = 0; i < 4; i++) {
            Key key = CryptoLib.recoverFromSignature(i, sig, messageHash);
            if (key != null && key.asInteger().equals(publicKey)) {
                recId = i;
                break;
            }
        }

        if (recId == -1) {
            throw new AdeniumException("Could not construct a recoverable key.");
        }

        int headerByte = recId + 53;

        // 1 header + 32 bytes for R + 32 bytes for S
        byte[] r = toBytesPadded(sig.getR(), 32);
        byte[] s = toBytesPadded(sig.getS(), 32);

        return new RecoverableSignature((byte) headerByte, r, s);
    }

    private byte[] toBytesPadded(BigInteger integer, int length) throws AdeniumException {
        byte result[] = Utils.toBytesPadded(integer, length);

        if (result.length > length) {
            throw new AdeniumException("result is too big.");
        }

        return result;
    }

    /**
     * Returns public key from the given private key.
     *
     * @param privKey the private key to derive the public key from
     * @return BigInteger encoded public key
     */
    public static Key publicKeyFromPrivate(BigInteger privKey) {
        ECPoint point = publicPointFromPrivate(privKey);

        return new ECPublicKey(point.getEncoded(false));
    }

    /**
     * Returns public key point from the given private key.
     *
     * @param privKey the private key to derive the public key from
     * @return ECPoint public key
     */
    public static ECPoint publicPointFromPrivate(BigInteger privKey) {
        /*
         * TODO: FixedPointCombMultiplier currently doesn't support scalars longer than the group
         * order, but that could change in future versions.
         */
        if (privKey.bitLength() > CryptoLib.getCurve().getN().bitLength()) {
            privKey = privKey.mod(CryptoLib.getCurve().getN());
        }

        return new FixedPointCombMultiplier().multiply(CryptoLib.getCurve().getG(), privKey);
    }

    /**
     * Returns public key point from the given curve.
     *
     * @param bits representing the point on the curve
     * @return BigInteger encoded public key
     */
    public static BigInteger publicFromPoint(byte[] bits) {
        return new BigInteger(1, Arrays.copyOfRange(bits, 1, bits.length)); // remove prefix
    }
}
