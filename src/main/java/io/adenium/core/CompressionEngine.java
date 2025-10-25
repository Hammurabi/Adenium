package io.adenium.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class CompressionEngine {
    public static byte[] compress(byte data[], int level) throws IOException {
        Deflater deflater = new Deflater(level);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        DeflaterOutputStream outputStream = new DeflaterOutputStream(output, deflater);

        outputStream.write(data);
        output.flush();
        output.close();

        return output.toByteArray();
    }
}
