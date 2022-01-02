package io.adenium.utils;

import io.adenium.exceptions.WolkenException;

public class Assertions {
    public static void assertTrue(boolean a, String message) throws WolkenException {
        if (a) {
            return;
        }

        throw new WolkenException(message);
    }

    public static void assertEquals(int a, int b, String message) throws WolkenException {
        if (a == b) {
            return;
        }

        throw new WolkenException(message);
    }

    public static void assertGreaterThan(int a, int b, String message) throws WolkenException {
        if (a > b) {
            return;
        }

        throw new WolkenException(message);
    }

    public static void assertGreaterThanOrEquals(int a, int b, String message) throws WolkenException {
        if (a >= b) {
            return;
        }

        throw new WolkenException(message);
    }

    public static void assertLessThan(int a, int b, String message) throws WolkenException {
        if (a < b) {
            return;
        }

        throw new WolkenException(message);
    }

    public static void assertLessThanOrEquals(int a, int b, String message) throws WolkenException {
        if (a <= b) {
            return;
        }

        throw new WolkenException(message);
    }
}
