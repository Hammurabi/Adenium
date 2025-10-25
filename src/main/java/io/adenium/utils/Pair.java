package io.adenium.utils;

public class Pair<A, B> {
    private final A a;
    private final B b;

    public Pair(A a, B b)
    {
        this.a = a;
        this.b = b;
    }

    public A getFirst()
    {
        return a;
    }

    public B getSecond()
    {
        return b;
    }
}
