package io.adenium.wallet;

import io.adenium.crypto.ec.ECKeypair;
import io.adenium.crypto.ec.ECPrivateKey;
import io.adenium.crypto.ec.ECPublicKey;
import io.adenium.exceptions.AdeniumException;
import io.adenium.crypto.AESResult;
import io.adenium.crypto.CryptoUtil;
import io.adenium.crypto.Key;
import io.adenium.utils.FileService;
import io.adenium.utils.HashUtil;
import io.adenium.utils.Utils;
import io.adenium.utils.VarInt;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

public class BasicWallet {
    private Key         publicKey;
    private long        nonce;
    private FileService fileService;

    public BasicWallet(Key publicKey, FileService service) {
        this.publicKey  = publicKey;
        this.fileService= service;
        this.nonce      = 0;
    }

    public BasicWallet(FileService newFile) throws IOException, AdeniumException {
        InputStream stream = fileService.openFileInputStream();
        int version = VarInt.readCompactUInt32(false, stream);
        byte pub[]  = new byte[65];
        int read = stream.read(pub);
        if (read != pub.length) {
            throw new AdeniumException("could not read entire public key.");
        }
        byte nonceBytes[] = new byte[8];
        read = stream.read(nonceBytes);
        nonce = Utils.makeLong(nonceBytes);
        publicKey = new ECPublicKey(pub);
        stream.close();
    }

    public Key getPrivateKey() {
        return getPrivateKey(null);
    }

    public Key getPrivateKey(char password[]) {
        try {
            InputStream stream = fileService.openFileInputStream();
            byte versionBytes[] = new byte[4];
            int read = stream.read(versionBytes);
            int version = Utils.makeInt(versionBytes);
            byte pub[]  = new byte[65];
            read = stream.read(pub);
            if (read != pub.length) {
                throw new AdeniumException("could not read entire public key.");
            }
            byte nonceBytes[] = new byte[8];
            read = stream.read(nonceBytes);
            byte salt[] = new byte[8];
            read = stream.read(salt);
            byte iv[] = new byte[16];
            read = stream.read(iv);
            byte enc[] = new byte[48];
            read = stream.read(enc);

            byte key[] = CryptoUtil.aesDecrypt(enc, CryptoUtil.generateSecretForAES(password, salt), iv);
            stream.close();

            return new ECPrivateKey(key);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void incrementNonce() {
        try {
            InputStream stream = fileService.openFileInputStream();
            byte allBytes[] = new byte[149];
            stream.read(allBytes);
            stream.close();

            byte nonce[] = Utils.takeApartLong(Utils.makeLong(allBytes, 69) + 1);
            allBytes[69] = nonce[0];
            allBytes[70] = nonce[1];
            allBytes[71] = nonce[2];
            allBytes[72] = nonce[3];
            allBytes[73] = nonce[4];
            allBytes[74] = nonce[5];
            allBytes[75] = nonce[6];
            allBytes[76] = nonce[7];

            OutputStream outputStream = fileService.openFileOutputStream();
            outputStream.write(allBytes);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static BasicWallet generateWallet(FileService service) throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidParameterSpecException, InvalidKeySpecException, IOException {
        return generateWallet(service, new char[] {0, 0, 0, 0});
    }

    public static BasicWallet generateWallet(FileService service, char password[]) throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidParameterSpecException, InvalidKeySpecException, IOException {
        if (!service.exists()) {
            byte random[] = new SecureRandom().generateSeed(1024);

            byte privateKey[] = HashUtil.sha256d(random);
            Key publicKey = ECKeypair.publicKeyFromPrivate(new BigInteger(1, privateKey));

            byte salt[]                 = new byte[8];
            new SecureRandom().nextBytes(salt);
            SecretKey encryptionKey     = CryptoUtil.generateSecretForAES(password, salt);
            AESResult result            = CryptoUtil.aesEncrypt(privateKey, encryptionKey);

            OutputStream stream         = service.openFileOutputStream();

            // write a version number
            VarInt.writeCompactUInt32(1, false, stream);
            // does not need to be encrypted (65 bytes)
            stream.write(publicKey.getEncoded());
            // write the current nonce
            Utils.writeLong(0, stream);
            // does not need to be secret (8 bytes)
            stream.write(salt);
            // does not need to be secret (16 bytes)
            stream.write(result.getIv());
            // must be encrypted (48 bytes)
            stream.write(result.getEncryptionResult());

            stream.flush();
            stream.close();

            return new BasicWallet(publicKey, service);
        }

        return null;
    }

    public long getNonce() {
        return nonce;
    }
}
