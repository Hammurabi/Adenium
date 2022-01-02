package io.adenium.utils;

import java.util.*;

public class PriorityHashQueue<T extends Comparable<T>> implements HashQueue<T> {
    private Map<ByteArray, Entry<T>>entryMap;
    private Queue<Entry<T>>         queue;
    private Comparator<Entry<T>>    comparator;
    private long                    byteCount;
    private Callable<Integer, T>    sizeManager;

    public PriorityHashQueue() {
        this(new DefaultComparator<>(), a -> { return 0; });
    }

    public PriorityHashQueue(Callable<Integer, T> sizeManager) {
        this(new DefaultComparator<>(), sizeManager);
    }

    public PriorityHashQueue(Comparator<Entry<T>> comparator, Callable<Integer, T> sizeManager) {
        queue       = new PriorityQueue<>(comparator);
        entryMap    = new HashMap<>();
        this.comparator = comparator;
    }

    @Override
    public boolean containsKey(ByteArray hash) {
        return entryMap.containsKey(hash);
    }

    @Override
    public void removeTails(int newLength, VoidCallableTY<T, byte[]> callable) {
        PriorityHashQueue<T> newQueue = new PriorityHashQueue<>(comparator, sizeManager);

        while (!isEmpty() && newQueue.size() < newLength) {
            Entry<T> e = queue.poll();
            newQueue.add(e.element, e.hash);
        }

        this.entryMap   = newQueue.entryMap;
        this.queue      = newQueue.queue;
        this.byteCount  = newQueue.byteCount;
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

        Entry<T> entry = queue.poll();
        entryMap.remove(entry.hash);

        byteCount -= sizeManager.call(entry.element);
        return entry.element;
    }

    @Override
    public T pop() {
        return null;
    }

    @Override
    public T peek() {
        if (queue.isEmpty()) {
            return null;
        }

        return queue.peek().element;
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public long byteCount() {
        return byteCount;
    }

    private static class DefaultComparator<T extends Comparable<T>> implements Comparator<Entry<T>> {
        @Override
        public int compare(Entry<T> a, Entry<T> b) {
            return a.compareTo(b);
        }
    }

    private static class Entry<T extends Comparable<T>> implements Comparable<Entry<T>> {
        private T       element;
        private byte[]  hash;

        @Override
        public int compareTo(Entry<T> b) {
            return element.compareTo(b.element);
        }
    }
}
