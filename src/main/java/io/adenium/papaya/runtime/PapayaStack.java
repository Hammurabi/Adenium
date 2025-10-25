package io.adenium.papaya.runtime;

import io.adenium.exceptions.PapayaException;

import java.util.Stack;

public class PapayaStack<T> {
    private Stack<T> stack;

    public PapayaStack() {
        stack = new Stack<>();
    }

    public void push(T element) {
        if (element == null) {
            return;
        }

        stack.push(element);
    }

    public T peek() throws PapayaException {
        if (isEmpty()) {
            throw new PapayaException("EmptyStackException caught");
        }

        return stack.peek();
    }

    public T pop() throws PapayaException {
        if (isEmpty()) {
            throw new PapayaException("EmptyStackException caught");
        }

        return stack.pop();
    }

    public void dup() throws PapayaException {
        if (isEmpty()) {
            throw new PapayaException("EmptyStackException caught");
        }

        stack.push(stack.peek());
    }

    public void dup(int element) throws PapayaException {
        int index = stack.size() - element;

        if (index < 0) {
            throw new PapayaException("EmptyStackException caught");
        }

        stack.push(stack.get(index));
    }

    public T peek(int element) throws PapayaException {
        int index = stack.size() - element;

        if (index < 0) {
            throw new PapayaException("EmptyStackException caught");
        }

        return stack.get(index);
    }

    public PapayaStack<T> rot() throws PapayaException {
        swap(1, 2);

        return this;
    }

    public void rot(int index) {
    }

    public void swap(int i, int j) throws PapayaException {
        int a = stack.size() - i;
        int b = stack.size() - j;

        if (a < 0 || b < 0) {
            throw new PapayaException("EmptyStackException caught");
        }

        T temp = stack.get(stack.size() - a);
        stack.set(stack.size() - a, stack.get(stack.size() - b));
        stack.set(stack.size() - b, temp);
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public int getSize() {
        return stack.size();
    }
}
