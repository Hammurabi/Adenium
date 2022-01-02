package io.adenium.exceptions;

public class WolkenTimeoutException extends WolkenException {
    private static final long serialVersionUID = -7341539217125586945L;

    public WolkenTimeoutException(String msg) {
        super(msg);
    }
}
