package io.adenium.exceptions;

public class UndefClassException extends PapayaException {
    public UndefClassException(String msg) {
        super(msg);
    }

    public UndefClassException(Throwable msg) {
        super(msg);
    }
}
