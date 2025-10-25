package io.adenium;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.adenium.utils.BitInputStream;
import io.adenium.utils.BitOutputStream;
import io.adenium.utils.Utils;

import java.io.IOException;

public class Bitstreams {
    @Test
    void bitStreamReadWrite() throws IOException {
        int testBytes[] = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

        BitOutputStream bitOutputStream = new BitOutputStream();
        for (int x = 0; x < testBytes.length; x++) {
            for (int i = 0; i < 4; i ++) {
                bitOutputStream.write(Utils.getBit(testBytes[x], i));
            }
        }

        bitOutputStream.flush();

        BitInputStream bitInputStream = new BitInputStream(bitOutputStream.toByteArray());
        byte bitsBuffer[] = new byte[4];
        for (int i = 0; i < testBytes.length; i ++) {
            for (int b = 0; b < bitsBuffer.length; b ++) {
                int read = bitInputStream.read();;

                Assertions.assertNotEquals(-1, read);

                bitsBuffer[b] = (byte) read;
            }

            Assertions.assertEquals(testBytes[i], Utils.makeByte(bitsBuffer));
        }

        bitOutputStream.close();
        bitInputStream.close();

        bitOutputStream = new BitOutputStream();

        // each of these integers can be represented by 19 bits
        int testStreams[] = {524287, 524286, 524285, 524284, 524283, 524282, 524281, 524280, 524279, 524269};

        for (int i = 0; i < testStreams.length; i ++) {
            for (int bit = 0; bit < 19; bit ++) {
                bitOutputStream.write(Utils.getBit(testStreams[i], bit));
            }

        }

        bitOutputStream.flush();
        bitInputStream  = new BitInputStream(bitOutputStream.toByteArray());
        bitsBuffer      = new byte[19];

        for (int i = 0; i < testStreams.length; i ++) {
            Assertions.assertEquals(bitsBuffer.length, bitInputStream.read(bitsBuffer));
            Assertions.assertEquals(testStreams[i], Utils.makeByte(bitsBuffer));
        }
    }
}
