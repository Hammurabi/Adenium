package io.adenium.wallet;

import io.adenium.crypto.ec.ECKeypair;
import io.adenium.crypto.ec.ECPrivateKey;
import io.adenium.crypto.ec.ECPublicKey;
import io.adenium.encoders.Base16;
import io.adenium.encoders.Base58;
import io.adenium.exceptions.WolkenException;
import org.json.JSONObject;
import io.adenium.core.Address;
import io.adenium.crypto.AESResult;
import io.adenium.crypto.CryptoUtil;
import io.adenium.crypto.Key;
import io.adenium.crypto.Keypair;
import io.adenium.utils.Utils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

public class Wallet {
    private final String  name;
    private final byte    privateKey[];
    private final Key     publicKey;
    private final Address address;
    private long          nonce;

    public Wallet(String dump) throws WolkenException {
        JSONObject json = new JSONObject(dump);
        if (json.has("name")) {
            throw new WolkenException("wallet requires the attribute 'name' to be present.");
        }
        if (json.has("private")) {
            throw new WolkenException("wallet requires the attribute 'private' to be present.");
        }
        if (json.has("public")) {
            throw new WolkenException("wallet requires the attribute 'public' to be present.");
        }
        if (json.has("address")) {
            throw new WolkenException("wallet requires the attribute 'address' to be present.");
        }
        if (json.has("nonce")) {
            throw new WolkenException("wallet requires the attribute 'nonce' to be present.");
        }
        if (!Base16.isEncoded(json.getString("private"))) {
            throw new WolkenException("wallet requires the attribute 'private' to be base16 encoded.");
        }
        if (!Base16.isEncoded(json.getString("public"))) {
            throw new WolkenException("wallet requires the attribute 'public' to be base16 encoded.");
        }
        if (!Base16.isEncoded(json.getString("address"))) {
            throw new WolkenException("wallet requires the attribute 'address' to be base16 encoded.");
        }
        if (!Address.isValidAddress(Base16.decode(json.getString("address")))) {
            throw new WolkenException("wallet requires the attribute 'address' to be a valid address.");
        }
        if (!json.get("nonce").toString().matches("\\d+")) {
            throw new WolkenException("wallet requires the attribute 'nonce' to be a valid unsigned integer.");
        }


        this.name       = json.getString("name");
        this.privateKey = Base16.decode(json.getString("private"));
        this.publicKey  = new ECPublicKey(Base16.decode(json.getString("public")));
        this.address    = Address.fromFormatted(Base16.decode(json.getString("address")));
        this.nonce      = json.getLong("nonce");
    }

    public static Wallet fromBytes(String name, byte array[]) {
        boolean isEncrypted = array[4] == 1;
        int privKeyLen      = isEncrypted ? 72 : 32;

        Key publicKey = new ECPublicKey(Utils.trim(array, 5 + privKeyLen, 65));

        return new Wallet(name, Utils.trim(array, 5, privKeyLen), publicKey, Address.fromKey(publicKey), Utils.makeLong(array, 5 + privKeyLen + 65));
    }

    public Wallet(String name, byte[] privateKey, Key publicKey, Address address, long nonce) {
        this.name = name;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.address = address;
        this.nonce = nonce;
    }

    public Wallet(String name, byte[] pass) throws WolkenException {
        this.name = name;
        byte salt[] = CryptoUtil.makeSalt();
        try {
            Key privKey         = new ECPrivateKey();
            if (pass == null) {
                this.privateKey = privKey.getEncoded();
            } else {
                char password[]     = Utils.makeChars(CryptoUtil.expand(pass, 48));
                SecretKey secretKey = CryptoUtil.generateSecretForAES(password, salt);
                AESResult result    = CryptoUtil.aesEncrypt(privKey.getEncoded(), secretKey);

                this.privateKey     = Utils.concatenate(salt, result.getIv(), result.getEncryptionResult());
            }
            this.publicKey      = ECKeypair.publicKeyFromPrivate(privKey.asInteger());
            this.address        = Address.fromKey(publicKey);
        } catch (InvalidKeySpecException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidParameterSpecException e) {
            throw new WolkenException(e);
        }
    }

    public Keypair getKeypairForSigning(byte pass[]) throws WolkenException {
        byte privateBytes[] = privateKey;
        if (pass != null) {
            char password[]     = Utils.makeChars(CryptoUtil.expand(pass, 48));

            byte salt[] = Utils.trim(privateBytes, 0, 8);
            byte iv[]   = Utils.trim(privateBytes, 8, 16);
            byte enc[]  = Utils.trim(privateBytes, 24, 48);

            try {
                privateBytes = CryptoUtil.aesDecrypt(enc, CryptoUtil.generateSecretForAES(password, salt), iv);
            } catch (InvalidKeySpecException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
                throw new WolkenException(e);
            }
        }

        return new ECKeypair(new ECPrivateKey(privateBytes), publicKey);
    }

    public String getName() {
        return name;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public Key getPublicKey() {
        return publicKey;
    }

    public Address getAddress() {
        return address;
    }

    public long getNonce() {
        return nonce;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("private", Base16.encode(privateKey));
        json.put("public", Base16.encode(publicKey.getEncoded()));
        json.put("address", Base58.encode(address.getFormatted()));
        json.put("nonce", nonce);

        return json;
    }

    public byte[] asByteArray() {
        return Utils.concatenate(Utils.takeApart(1), new byte[] {(byte) (isEncrypted() ? 1 : 0)}, privateKey, publicKey.getEncoded(), Utils.takeApartLong(nonce));
    }

    public boolean isEncrypted() {
        return getPrivateKey().length != 32;
    }

    public static byte[] encrypt(byte pass[], byte key[]) throws WolkenException {
        try {
            byte salt[]         = CryptoUtil.makeSalt();
            char password[]     = Utils.makeChars(CryptoUtil.expand(pass, 48));
            SecretKey secretKey = CryptoUtil.generateSecretForAES(password, salt);
            AESResult result    = CryptoUtil.aesEncrypt(key, secretKey);

            return Utils.concatenate(salt, result.getIv(), result.getEncryptionResult());
        } catch (Exception e) {
            throw new WolkenException(e);
        }
    }

    public Wallet encrypt(byte[] pass) throws WolkenException {
        return new Wallet(name, encrypt(pass, privateKey), publicKey, address, nonce);
    }

    public Wallet changePassphrase(byte oldPass[], byte newPass[]) throws WolkenException {
        if (!isEncrypted()) {
            throw new WolkenException("wallet '"+name+"' is not encrypted.");
        }

        char password[]     = Utils.makeChars(CryptoUtil.expand(oldPass, 48));

        byte salt[] = Utils.trim(privateKey, 0, 8);
        byte iv[]   = Utils.trim(privateKey, 8, 16);
        byte enc[]  = Utils.trim(privateKey, 24, 48);

        try {
            byte privateKey[] = CryptoUtil.aesDecrypt(enc, CryptoUtil.generateSecretForAES(password, salt), iv);

            if (!publicKey.equals(ECKeypair.publicKeyFromPrivate(new BigInteger(1, privateKey)))) {
                throw new WolkenException("incorrect decryption key provided for wallet '" + name + "'.");
            }

            return new Wallet(name, encrypt(newPass, privateKey), publicKey, address, nonce);
        } catch (InvalidKeySpecException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new WolkenException(e);
        }
    }
}
