package io.adenium.exceptions;

public class AdeniumException extends Exception {
    private static final long serialVersionUID = 5112431926980310096L;

    public AdeniumException(){
    }

    public AdeniumException(String msg)
    {
        super(msg);
    }

    public AdeniumException(Throwable cause)
    {
        super(cause);
    }
}
