package io.adenium;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.adenium.crypto.Keypair;
import io.adenium.crypto.Signature;
import io.adenium.exceptions.WolkenException;

import java.security.SecureRandom;

public class EcLib {
    @Test
    void verifyMatchingSignatures() throws WolkenException {
        SecureRandom random = new SecureRandom();
        byte message[] = new byte[256];

        for (int i = 0; i < 2000; i ++) {
            random.nextBytes(message);

            // make a random keypair
            Keypair keypair1 = Keypair.ellipticCurvePair();
            Keypair keypair2 = Keypair.ellipticCurvePair();
            // sign
            Signature a = keypair1.sign(message);
            Signature b = keypair2.sign(message);
            // verify signature
            Assertions.assertTrue(a.checkSignature(message, keypair1.getPublicKey()), "could not verify signature A against pair A");
            Assertions.assertTrue(b.checkSignature(message, keypair2.getPublicKey()), "could not verify signature B against pair B");
            Assertions.assertFalse(a.checkSignature(message, keypair2.getPublicKey()), "verified signature A against pair B");
            Assertions.assertFalse(b.checkSignature(message, keypair1.getPublicKey()), "verified signature B against pair A");
        }
    }

    @Test
    void verifyDisproportionateSignatures() throws WolkenException {
        SecureRandom random = new SecureRandom();
        byte message1[] = new byte[256];
        byte message2[] = new byte[256];

        for (int i = 0; i < 2000; i ++) {
            random.nextBytes(message1);
            random.nextBytes(message2);

            // make a random keypair
            Keypair keypair1 = Keypair.ellipticCurvePair();
            Keypair keypair2 = Keypair.ellipticCurvePair();
            // sign
            Signature a = keypair1.sign(message1);
            Signature b = keypair2.sign(message2);
            // verify signature
            Assertions.assertTrue(a.checkSignature(message1, keypair1.getPublicKey()), "could not verify signature A against pair A");
            Assertions.assertTrue(b.checkSignature(message2, keypair2.getPublicKey()), "could not verify signature B against pair B");
            Assertions.assertFalse(a.checkSignature(message1, keypair2.getPublicKey()), "verified signature A against pair B");
            Assertions.assertFalse(b.checkSignature(message2, keypair1.getPublicKey()), "verified signature B against pair A");
        }
    }

    @Test
    void recoverKeyFromSignature() throws WolkenException {
        SecureRandom random = new SecureRandom();
        byte message[] = new byte[256];

        for (int i = 0; i < 2000; i ++) {
            random.nextBytes(message);

            // make a random keypair
            Keypair keypair = Keypair.ellipticCurvePair();
            // sign
            Signature signature = keypair.sign(message);
            // verify signature
            Assertions.assertTrue(signature.checkSignature(message, keypair.getPublicKey()), "could not verify signature");
            // verify key
            Assertions.assertEquals(0, signature.recover(message).asInteger().compareTo(keypair.getPublicKey().asInteger()), "could not recover public key from signature");
        }
    }
}
