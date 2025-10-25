package io.adenium;

import org.junit.jupiter.api.Test;
import io.adenium.core.BlockHeader;
import io.adenium.core.PrunedBlock;
import io.adenium.crypto.CryptoLib;
import io.adenium.utils.HashUtil;
import io.adenium.utils.Utils;

import java.util.LinkedList;
import java.util.Queue;

public class CompressionBenchmarks {
    @Test
    public void compressRandomBits() {
        CryptoLib.getInstance();
        int originalSize    = 0;
        int compressedSize  = 0;

        int rounds = 500;

        for (int i = 0; i < rounds; i ++) {
            BlockHeader header = new BlockHeader(1, i, HashUtil.sha256d(Utils.takeApart(i)), HashUtil.sha256d(Utils.takeApart(i + 2 * i)), 250, 250);
            Queue<byte[]> tx = new LinkedList<>();
            Queue<byte[]> ev = new LinkedList<>();
            for (int t = 0; t < 3600; t ++) {
                tx.add(HashUtil.sha256d(Utils.takeApart(t + 1 + i * 23)));
            }
            for (int e = 0; e < 7200; e ++) {
                ev.add(HashUtil.sha256d(Utils.takeApart(e + 2 + i * 15)));
            }
            PrunedBlock prunedBlock = new PrunedBlock(header , tx, ev);
            byte uncompressed[] = prunedBlock.asByteArray();
            originalSize    += uncompressed.length;
            byte compressed[]   = prunedBlock.asByteArray(9);
            compressedSize  += compressed.length;
        }

        int reduction = originalSize - compressedSize;
        double percent= (double) reduction / originalSize;

        System.out.println("normal    size: " + originalSize + "%");
        System.out.println("compresed size: " + compressedSize + "%");
        System.out.println("average compression: " + percent + "%");
    }
}
