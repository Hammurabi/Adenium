package io.adenium.exceptions;

public class PapayaException extends Exception {
    private static final long serialVersionUID = 5112431926980310096L;

    public PapayaException(String msg)
    {
        super(msg);
    }
    public PapayaException(Throwable cause)
    {
        super(cause);
    }
}
