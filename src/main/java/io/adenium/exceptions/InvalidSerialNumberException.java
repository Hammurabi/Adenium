package io.adenium.exceptions;

public class InvalidSerialNumberException extends WolkenException {
    private static final long serialVersionUID = 4000853491597138082L;

    public InvalidSerialNumberException(String msg) {
        super(msg);
    }

    public InvalidSerialNumberException(Throwable throwable)
    {
        super(throwable);
    }
}
