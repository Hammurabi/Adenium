package io.adenium.utils;

public interface Callable<Return, Argument> {
    public Return call(Argument argument);
}
