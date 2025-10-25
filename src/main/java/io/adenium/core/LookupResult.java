package io.adenium.core;

public class LookupResult<T> {
    private final T         result;
    private final boolean   exists;
    public LookupResult(T result, boolean exists)
    {
        this.result = result;
        this.exists = exists;
    }

    public T getResult()
    {
        return result;
    }

    public boolean exists()
    {
        return exists;
    }
}
