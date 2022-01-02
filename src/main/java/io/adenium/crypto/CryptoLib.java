package io.adenium.crypto;

import io.adenium.crypto.ec.ECPublicKey;
import io.adenium.crypto.ec.ECSig;
import io.adenium.exceptions.WolkenException;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;
import io.adenium.utils.Assertions;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

public class CryptoLib {
    private static final CryptoLib secureLib = new CryptoLib();
    private static X9ECParameters PARAMS;
    private static ECDomainParameters CURVE;
    private static BigInteger HALF_CURVE_ORDER;

    public static CryptoLib getInstance() {
        return secureLib;
    }

    public static X9ECParameters getParams() {
        return PARAMS;
    }

    public static ECDomainParameters getCurve() {
        return CURVE;
    }

    public static BigInteger getHalfCurveOrder() {
        return HALF_CURVE_ORDER;
    }

    protected CryptoLib() {
        Security.addProvider(new BouncyCastleProvider());
        PARAMS = CustomNamedCurves.getByName("secp256k1");//SECNamedCurves.getByName("secp256k1");
        CURVE = new ECDomainParameters(PARAMS.getCurve(), PARAMS.getG(), PARAMS.getN(), PARAMS.getH());
        HALF_CURVE_ORDER = PARAMS.getN().shiftRight(1);
    }

    public static Key recoverFromSignature(int recId, ECSig sig, byte[] message) throws WolkenException {
        Assertions.assertTrue(recId >= 0, "recId must be positive");
        Assertions.assertTrue(sig.getR().signum() >= 0, "r must be positive");
        Assertions.assertTrue(sig.getS().signum() >= 0, "s must be positive");
        Assertions.assertTrue(message != null, "message cannot be null");

        // 1.0 For j from 0 to h   (h == recId here and the loop is outside this function)
        //   1.1 Let x = r + jn
        BigInteger n = CURVE.getN(); // Curve order.
        BigInteger i = BigInteger.valueOf((long) recId / 2);
        BigInteger x = sig.getR().add(i.multiply(n));
        //   1.2. Convert the integer x to an octet string X of length mlen using the conversion
        //        routine specified in Section 2.3.7, where mlen = ⌈(log2 p)/8⌉ or mlen = ⌈m/8⌉.
        //   1.3. Convert the octet string (16 set binary digits)||X to an elliptic curve point R
        //        using the conversion routine specified in Section 2.3.4. If this conversion
        //        routine outputs "invalid", then do another iteration of Step 1.
        //
        // More concisely, what these points mean is to use X as a compressed public key.
        BigInteger prime = SecP256K1Curve.q;
        if (x.compareTo(prime) >= 0) {
            // Cannot have point co-ordinates larger than this as everything takes place modulo Q.
            return null;
        }
        // Compressed keys require you to know an extra bit of data about the y-coord as there are
        // two possibilities. So it's encoded in the recId.
        ECPoint R = decompressKey(x, (recId & 1) == 1);
        //   1.4. If nR != point at infinity, then do another iteration of Step 1 (callers
        //        responsibility).
        if (!R.multiply(n).isInfinity()) {
            return null;
        }
        //   1.5. Compute e from M using Steps 2 and 3 of ECDSA signature verification.
        BigInteger e = new BigInteger(1, message);
        //   1.6. For k from 1 to 2 do the following.   (loop is outside this function via
        //        iterating recId)
        //   1.6.1. Compute a candidate public key as:
        //               Q = mi(r) * (sR - eG)
        //
        // Where mi(x) is the modular multiplicative inverse. We transform this into the following:
        //               Q = (mi(r) * s ** R) + (mi(r) * -e ** G)
        // Where -e is the modular additive inverse of e, that is z such that z + e = 0 (mod n).
        // In the above equation ** is point multiplication and + is point addition (the EC group
        // operator).
        //
        // We can find the additive inverse by subtracting e from zero then taking the mod. For
        // example the additive inverse of 3 modulo 11 is 8 because 3 + 8 mod 11 = 0, and
        // -3 mod 11 = 8.
        BigInteger eInv = BigInteger.ZERO.subtract(e).mod(n);
        BigInteger rInv = sig.getR().modInverse(n);
        BigInteger srInv = rInv.multiply(sig.getS()).mod(n);
        BigInteger eInvrInv = rInv.multiply(eInv).mod(n);
        ECPoint q = ECAlgorithms.sumOfTwoMultiplies(CURVE.getG(), eInvrInv, R, srInv);

        byte[] qBytes = q.getEncoded(false);
        // We remove the prefix
//        new BigInteger(1, Arrays.copyOfRange(qBytes, 1, qBytes.length));
        return new ECPublicKey(qBytes);
    }

    /** Decompress a compressed public key (x co-ord and low-bit of y-coord). */
    private static ECPoint decompressKey(BigInteger xBN, boolean yBit) {
        X9IntegerConverter x9 = new X9IntegerConverter();
        byte[] compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(CURVE.getCurve()));
        compEnc[0] = (byte) (yBit ? 0x03 : 0x02);
        return CURVE.getCurve().decodePoint(compEnc);
    }

    /**
     * @param s The secret 32 bytes to initialize the private key.
     * @param curveName The curvename to use with key creation (secp256k1 by default).
     * @return A PrivateKey object.
     */
    public final PrivateKey genPrivateKey(BigInteger s, String curveName) throws WolkenException {
        try {
            ECParameterSpec ecParameterSpec = ECNamedCurveTable.getParameterSpec(curveName);

            ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(s, ecParameterSpec);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            BCECPrivateKey key = (BCECPrivateKey) keyFactory.generatePrivate(privateKeySpec);

            if (key.getD().bitLength() > CURVE.getN().bitLength()) {
                while (key.getD().bitLength() > CURVE.getN().bitLength()) {
                    key = (BCECPrivateKey) genPrivateKey(key.getD().mod(CURVE.getN()), curveName);
                }

                if (key.getD().equals(BigInteger.ZERO))
                    throw new WolkenException("Resulting private key is ZERO.");

                throw new WolkenException("P%N==0.");
            }

            if (key.getD().equals(BigInteger.ZERO))
                throw new WolkenException("Resulting private key is ZERO.");

            return key;
        } catch (WolkenException e) {
            throw e;
        } catch (NoSuchAlgorithmException e) {
            throw new WolkenException(e.getMessage());
        } catch (InvalidKeySpecException e) {
            throw new WolkenException(e.getMessage());
        }
    }

    public final PublicKey generatePublicKey(ECPoint pointUncompressed, String curveName)
    {
        ECParameterSpec ecParameterSpec = ECNamedCurveTable.getParameterSpec(curveName);

        ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(pointUncompressed, ecParameterSpec);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static final ECPoint ec_point_point2oct(byte data[], boolean compressed)
    {
        X9ECParameters params = SECNamedCurves.getByName("secp256k1");
        ECDomainParameters CURVE = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());

        ECPoint point = CURVE.getCurve().decodePoint(data);

        return point;
    }

    public BCECPublicKey derivePublicKey(BCECPrivateKey key) throws WolkenException {
        if (key == null) throw new WolkenException("cannot derivePublicKey public key from null private-key.");

        ECPoint point = new FixedPointCombMultiplier().multiply(CURVE.getG(), key.getD());

        return (BCECPublicKey) generatePublicKey(ec_point_point2oct(point.getEncoded(false), false), "secp256k1");
    }

    public BCECPublicKey derivePublicKey(BigInteger privateKey) throws WolkenException {
        BCECPrivateKey bcecPrivateKey = (BCECPrivateKey) genPrivateKey(privateKey, "secp256k1");
        return derivePublicKey(bcecPrivateKey);
    }

    /**
     * @param priv Private key to use for signing data.
     * @param data A double sha256 of the data to be signed.
     * @return A Valid signature byte array.
     */
    public final byte[] sign(BCECPrivateKey priv, byte data[]) throws WolkenException {
        try {
            java.security.Signature dsa = java.security.Signature.getInstance("SHA256withECDSA", "BC");

            dsa.initSign(priv);

            dsa.update(data);

            return dsa.sign();
        } catch ( Throwable throwable ) {
            throw new WolkenException(throwable);
        }
    }

    /**
     * @param pubkey Public key to be used for signature verification.
     * @param data A sha256d of the original signed data.
     * @param sig The signature to verify.
     * @return True only if the Public key is derived from the signing private key.
     */
    public final boolean verifySignature(BCECPublicKey pubkey, byte[] data, byte[] sig) throws WolkenException {
        try {
            java.security.Signature signature = java.security.Signature.getInstance("SHA256withECDSA");
            signature.initVerify(pubkey);

            signature.update(data);

            return signature.verify(sig);
        } catch (Throwable throwable) {
            throw new WolkenException(throwable);
        }
    }
}
