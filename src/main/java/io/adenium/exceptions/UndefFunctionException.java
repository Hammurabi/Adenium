package io.adenium.exceptions;

public class UndefFunctionException extends PapayaException {
    public UndefFunctionException(String msg) {
        super(msg);
    }

    public UndefFunctionException(Throwable msg) {
        super(msg);
    }
}
