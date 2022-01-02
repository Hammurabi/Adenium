package io.adenium.crypto;

import io.adenium.crypto.ec.ECKeypair;
import io.adenium.crypto.ec.ECPrivateKey;
import io.adenium.exceptions.WolkenException;

import java.security.SecureRandom;
import java.util.Random;

public abstract class Keypair {
    private final Key privateKey;
    private final Key publicKey;

    public Keypair(Key priv, Key pubk) {
        this.privateKey = priv;
        this.publicKey  = pubk;
    }

    public Key getPrivateKey() {
        return privateKey;
    }

    public Key getPublicKey() {
        return publicKey;
    }

    public abstract Signature sign(byte message[]) throws WolkenException;

    public static Keypair ellipticCurvePair() throws WolkenException {
        return ellipticCurvePair(new SecureRandom());
    }

    public static Keypair ellipticCurvePair(Random random) throws WolkenException {
        byte pkBytes[] = new byte[32];
        random.nextBytes(pkBytes);

        return ellipticCurvePair(new ECPrivateKey(pkBytes));
    }

    private static Keypair ellipticCurvePair(Key privateKey) throws WolkenException {
        return new ECKeypair(privateKey);
    }
}
