package io.adenium;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.adenium.core.Int256;
import io.adenium.utils.Utils;

import java.math.BigInteger;

public class Int256Tests {
    @Test
    void testInt256() {
        {
            for (int i = 0; i < 320; i ++) {
                long x          = ((long) Integer.MAX_VALUE * 2L) * i;

                Assertions.assertEquals(Long.toString(x), new Int256(x).toString());
            }
        }
        {
            for (int i = 0; i < 20; i ++) {
                long f          = (i * 1000) * 1000;
                long s          = (i * 1000) * 1000 + 1;

                Int256 first    = new Int256(f);
                Int256 second   = new Int256(s);
                long value      = first.add(second).asLong();
                Assertions.assertEquals(f + s, value);
            }
        }
        {
            for (int i = 0; i < 20; i ++) {
                long f          = (i * Integer.MAX_VALUE) * 23 + 1;
                long s          = (i * Integer.MAX_VALUE) * 23;

                Int256 first    = new Int256(f);
                Int256 second   = new Int256(s);
                long value      = first.sub(second).asLong();
                Assertions.assertEquals(f - s, value);
            }
        }
        {
            for (int i = 0; i < 20; i ++) {
                long f          = (i * Integer.MAX_VALUE) * 23;
                long s          = (i * Integer.MAX_VALUE) * 23 + i;

                Int256 first    = new Int256(f);
                Int256 second   = new Int256(s);

                long value      = first.sub(second).asLong();
                Assertions.assertEquals(f - s, value);
            }
        }
        {
            for (int i = 0; i < 1_000_000; i ++) {
                long f          = (i * Integer.MAX_VALUE) * 2;
                long s          = (i * Short.MAX_VALUE) * 16;

                Int256 first    = new Int256(f);
                Int256 second   = new Int256(s);

                long value      = first.mul(second).asLong();
                Assertions.assertEquals(f * s, value);
            }
        }
        {
            BigInteger a        = new BigInteger("2").pow(32);
            for (int i = 0; i < 32_000; i ++) {
                BigInteger b    = a.multiply(new BigInteger(Integer.toString(i)));

                byte aa[]       = a.toByteArray();
                byte ab[]       = b.toByteArray();

                Int256 first    = new Int256(Utils.pad(32 - aa.length, aa), false);
                Int256 second   = new Int256(Utils.pad(32 - ab.length, ab), false);

                Assertions.assertEquals(a.multiply(b).toString(), first.mul(second).toString());
            }
        }
    }
}
