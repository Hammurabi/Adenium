package io.adenium.exceptions;

public class InvalidOpcodeException extends PapayaException {
    public InvalidOpcodeException(String msg) {
        super(msg);
    }

    public InvalidOpcodeException(Throwable msg) {
        super(msg);
    }
}
