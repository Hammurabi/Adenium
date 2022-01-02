package io.adenium.crypto;

public class AESResult {
    private byte encryptionResult[];
    private byte iv[];

    public AESResult(byte[] encryptionResult, byte[] iv) {
        this.encryptionResult = encryptionResult;
        this.iv = iv;
    }

    public byte[] getEncryptionResult() {
        return encryptionResult;
    }

    public byte[] getIv() {
        return iv;
    }
}
