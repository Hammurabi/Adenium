package io.adenium.utils;

import java.util.*;

public class LinkedHashQueue<T> implements HashQueue<T> {
    private Map<ByteArray, Entry<T>>    entryMap;
    private List<Entry<T>>              queue;
    private long                        byteCount;
    private Callable<Long, T>           sizeManager;

    public LinkedHashQueue() {
        this((t) -> { return 0L; });
    }

    public LinkedHashQueue(Callable<Long, T> sizeManager) {
        queue       = new LinkedList<>();
        entryMap    = new HashMap<>();
        this.sizeManager = sizeManager;
    }

    @Override
    public boolean containsKey(ByteArray hash) {
        return entryMap.containsKey(hash);
    }

    @Override
    public void removeTails(int newLength, VoidCallableTY<T, byte[]> callable) {
        while (!isEmpty() && size() > newLength) {
            Entry<T> entry = queue.get(0);
            poll();

            callable.call(entry.element, entry.hash);
        }
    }

    @Override
    public T getByHash(ByteArray hash) {
        if (entryMap.containsKey(hash)) {
            return entryMap.get(hash).element;
        }

        return null;
    }

    @Override
    public void add(T element, byte[] hash) {
        Entry<T> entry  = new Entry<>();
        entry.element   = element;
        entry.hash      = hash;

        entryMap.put(ByteArray.wrap(hash), entry);
        queue.add(entry);

        byteCount += sizeManager.call(element);
    }

    @Override
    public T poll() {
        if (queue.isEmpty()) {
            return null;
        }

        Entry<T> entry = queue.get(0);
        queue.remove(0);
        entryMap.remove(ByteArray.wrap(entry.hash));

        byteCount -= sizeManager.call(entry.element);
        return entry.element;
    }

    @Override
    public T pop() {
        if (queue.isEmpty()) {
            return null;
        }

        Entry<T> entry = queue.get(queue.size() - 1);
        queue.remove(queue.size() - 1);
        entryMap.remove(ByteArray.wrap(entry.hash));

        byteCount -= sizeManager.call(entry.element);
        return entry.element;
    }

    @Override
    public T peek() {
        if (queue.isEmpty()) {
            return null;
        }

        return queue.get(0).element;
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public long byteCount() {
        return byteCount;
    }

    private static class Entry<T> {
        private T       element;
        private byte[]  hash;
    }
}
