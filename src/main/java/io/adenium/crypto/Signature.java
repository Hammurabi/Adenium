package io.adenium.crypto;

import io.adenium.exceptions.AdeniumException;
import io.adenium.serialization.SerializableI;

public abstract class Signature extends SerializableI {
    public abstract boolean checkSignature(byte[] originalMessage, Key publicKey);
    public Key recover(byte originalMessage[]) throws AdeniumException {
        return null;
    }
}
