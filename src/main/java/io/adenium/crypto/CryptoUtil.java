package io.adenium.crypto;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import io.adenium.utils.HashUtil;
import io.adenium.utils.Utils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;

public class CryptoUtil {
    public static SecretKey generateSecretForAES(char password[], byte salt[]) throws InvalidKeySpecException, NoSuchAlgorithmException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

    public static AESResult aesEncrypt(byte[] bytes, SecretKey secret) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidParameterSpecException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        AlgorithmParameters params = cipher.getParameters();
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
        return new AESResult(cipher.doFinal(bytes), iv);
    }

    public static byte[] aesDecrypt(byte[] bytes, SecretKey secret, byte iv[]) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));

        return cipher.doFinal(bytes);
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

    public static byte[] makeSalt() {
        byte salt[] = new byte[8];
        new SecureRandom().nextBytes(salt);

        return salt;
    }

    public static byte[] makeSecureBytes(int length) {
        byte bytes[] = new byte[length];
        new SecureRandom().nextBytes(bytes);

        return bytes;
    }

    public static byte[] expand(byte[] bytes, int length) {
        byte newBytes[] = null;

        if (length <= 20) {
            newBytes = HashUtil.ripemd160(bytes);
        } else if (length <= 32) {
            newBytes = HashUtil.sha256(bytes);
        } else if (length <= 64) {
            newBytes = HashUtil.sha512(bytes);
        }

        return Utils.trim(newBytes, 0, length);
    }
}
