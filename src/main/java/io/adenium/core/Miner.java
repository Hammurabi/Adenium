package io.adenium.core;

import io.adenium.exceptions.AdeniumException;
import io.adenium.utils.ChainMath;
import io.adenium.utils.HashUtil;

public class Miner extends AbstractMiner {
    public Miner(Address miningAddress) {
        super(miningAddress);
    }

    @Override
    public void mine(Block block) throws AdeniumException {
        BlockHeader header  = block.getBlockHeader();
        byte hash[]         = header.getHashCode();
        int nonce           = 0;
        byte headerBytes[]  = header.asByteArray();

        while (!ChainMath.validSolution(hash, header.getBits())) {
            ++ nonce;
            headerBytes[76] = (byte) (nonce >> 24 & 0xFF);
            headerBytes[77] = (byte) (nonce >> 16 & 0xFF);
            headerBytes[78] = (byte) (nonce >> 8 & 0xFF);
            headerBytes[79] = (byte) (nonce & 0xFF);
            hash            = HashUtil.sha256d(headerBytes);
        }

        header.setNonce(nonce);
    }
}
