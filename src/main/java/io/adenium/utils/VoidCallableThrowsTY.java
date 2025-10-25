package io.adenium.utils;

public interface VoidCallableThrowsTY<Argument, T extends Throwable, Y extends Throwable> {
    public void call(Argument argument) throws T, Y;
}
