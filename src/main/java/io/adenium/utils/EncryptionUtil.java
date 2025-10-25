package io.adenium.utils;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;

public class EncryptionUtil {
    public static KeyPair rsaKeyPair() throws NoSuchProviderException, NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");
        generator.initialize(2048);

        return generator.generateKeyPair();
    }

    public static byte[] rsaEncrypt(byte messageBytes[], byte[] pubKey) throws IOException, InvalidCipherTextException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        AsymmetricKeyParameter publicKey =
                (AsymmetricKeyParameter) PublicKeyFactory.createKey(pubKey);
        AsymmetricBlockCipher e = new RSAEngine();
        e = new org.bouncycastle.crypto.encodings.PKCS1Encoding(e);
        e.init(true, publicKey);

        return e.processBlock(messageBytes, 0, messageBytes.length);
    }

    public static byte[] rsaDecrypt(byte messageBytes[], byte[] privKey) throws IOException, InvalidCipherTextException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        AsymmetricKeyParameter privateKey =
                (AsymmetricKeyParameter) PrivateKeyFactory.createKey(privKey);
        AsymmetricBlockCipher e = new RSAEngine();
        e = new org.bouncycastle.crypto.encodings.PKCS1Encoding(e);
        e.init(false, privateKey);

        return e.processBlock(messageBytes, 0, messageBytes.length);
    }

    public static char[] secureRandomChars(int length) {
        SecureRandom random = new SecureRandom();
        char characters[] = new char[length];
        for (int i = 0; i < characters.length; i ++)
            characters[i] = (char) random.nextInt(65536);

        return characters;
    }

    public static byte[] secureRandomBytes(int length) {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[length];

        random.nextBytes(bytes);

        return bytes;
    }

    public static SecretKey generateSecretForAES(char password[], byte salt[]) throws InvalidKeySpecException, NoSuchAlgorithmException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    public static Pair<byte[], byte[]> aesEncrypt(byte[] bytes, SecretKey secret) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidParameterSpecException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        AlgorithmParameters params = cipher.getParameters();
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
        return new Pair<>(cipher.doFinal(bytes), iv);
    }

    public static byte[] aesDecrypt(byte[] bytes, SecretKey secret, byte iv[]) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));

        return cipher.doFinal(bytes);
    }
}
