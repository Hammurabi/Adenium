package io.adenium.core;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class TaskScheduler implements Runnable {
    private final Queue<Runnable>   tasks;
    private final ReentrantLock     mutex;

    public TaskScheduler() {
        tasks = new LinkedList<>();
        mutex = new ReentrantLock();
    }

    @Override
    public void run() {
        while (Context.getInstance().isRunning()) {
            if (hasTask()) {
                getTask().run();
            }
        }
    }

    private boolean hasTask() {
        mutex.lock();
        try {
            return !tasks.isEmpty();
        } finally {
            mutex.unlock();
        }
    }

    private Runnable getTask() {
        mutex.lock();
        try {
            return tasks.poll();
        } finally {
            mutex.unlock();
        }
    }

    public void runAsync(Runnable runnable) {
        mutex.lock();
        try {
            tasks.add(runnable);
        } finally {
            mutex.unlock();
        }
    }
}
