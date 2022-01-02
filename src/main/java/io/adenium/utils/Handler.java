package io.adenium.utils;

public class Handler<T> {
    private T t;

    public Handler(T t) {
        this.t = t;
    }

    public T get() {
        return t;
    }

    public void set(T t) {
        this.t = t;
    }
}
