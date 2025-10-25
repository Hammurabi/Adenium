package io.adenium.exceptions;

public class AdeniumTimeoutException extends AdeniumException {
    private static final long serialVersionUID = -7341539217125586945L;

    public AdeniumTimeoutException(String msg) {
        super(msg);
    }
}
