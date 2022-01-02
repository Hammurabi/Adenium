package io.adenium.papaya.runtime;

import java.math.BigInteger;

public class PapayaNumber extends PapayaObject {
    private final BigInteger    number;
    private boolean             allowSignedOperations;

    public PapayaNumber(long number, boolean allowSignedOperations) {
        this(fromLong(number, allowSignedOperations), allowSignedOperations);
    }

    public PapayaNumber(BigInteger number, boolean allowSignedOperations) {
        this.number                 = number;
        this.allowSignedOperations  = allowSignedOperations;
    }

    @Override
    public BigInteger asInt() {
        return number;
    }

    protected static BigInteger fromLong(long number, boolean allowSignedOperations) {
        if (allowSignedOperations) {
            return new BigInteger(Long.toString(number));
        }

        return new BigInteger(Long.toUnsignedString(number));
    }
}
