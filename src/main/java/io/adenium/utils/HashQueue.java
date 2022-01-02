package io.adenium.utils;

public interface HashQueue<T> {
    boolean containsKey(ByteArray hash);
    default void removeTails(int newLength) {
        removeTails(newLength, (a, b)->{});
    }
    void removeTails(int newLength, VoidCallableTY<T, byte[]> callable);
    T getByHash(ByteArray hash);
    void add(T element, byte hash[]);
    T poll();
    T pop();
    T peek();
    int size();
    // should return the amount of bytes in memory this queue is taking (if available).
    long byteCount();
    default boolean hasElements() {
        return !isEmpty();
    }
    default boolean isEmpty() { return size() != 0; }
}
