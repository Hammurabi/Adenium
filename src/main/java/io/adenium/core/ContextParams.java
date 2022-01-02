package io.adenium.core;

import io.adenium.exceptions.WolkenException;
import io.adenium.network.VersionInformation;
import io.adenium.utils.ChainMath;

import java.math.BigInteger;

public class ContextParams {
    private boolean isTestNet;
    private int     defaultBits;
    private int     loggingLevel;
    private int     maximumBytesTxInPendingState;
    private int     maximumBytesTxInRejectedState;
    private int     maximumBytesTxInFutureState;

    private BigInteger maximumTarget;

    public ContextParams(boolean testNet, int loggingLevel) throws WolkenException {
        this.isTestNet = testNet;

        if (testNet) {
            defaultBits = 0x1e00ffff;
        } else {
            defaultBits = 0x1d00ffff;
        }

        this.maximumTarget  = ChainMath.targetIntegerFromBits(defaultBits);
        this.loggingLevel   = loggingLevel;
    }

    public boolean isTestNet() {
        return isTestNet;
    }

    public int getMaxBlockWeight() {
        return 1_000_000;
    }

    public int getMaxBlockSize() {
        return getMaxBlockWeight();
    }

    public int getAverageBlockTime() {
        return 12;
    }

    public int getDefaultBits() {
        return defaultBits;
    }

    public byte[] getEmptyChainLink() {
        return new byte[20];
    }

    public int getCoinbaseLockTime() {
        return 7200;
    }

    public BigInteger getMaxTarget() {
        return maximumTarget;
    }

    public int getNonceLength() {
        return 24;
    }

    public int getDifficultyAdjustmentThreshold() {
        return 1800;
    }

    public long blocksPerYear() {
        return 2_625_000L;
    }

    public long getHalvingRate() {
        return blocksPerYear() * 4;
    }

    public long getMaxReward() {
        return getOneCoin();
    }

    /**
     *
     * Term: 37 halvings or roughly 148 years.
     *
     */
    public long getMaxCoin() {
        return 21_000_000__00_000_000_000L;
    }

    public long getOneCoin() {
        return 1___________00_000_000_000L;
    }

    public long getAliasRegistrationCost() {
        return 5_000L;
    }

    public byte getGenericMainnetAddressPrefix() {
        return 24;
    }

    public byte getGenericTestnetAddressPrefix() {
        return 43;
    }

    public byte getGenericAddressPrefix() {
        if (isTestNet) {
            getGenericTestnetAddressPrefix();
        }

        return getGenericMainnetAddressPrefix();
    }

    public byte getContractAddressPrefix() {
        if (isTestNet) {
            return 41;
        }

        return 51;
    }

    public int getBufferSize() {
        return 16384;
    }

    // this does not account for message headers.
    public int getMaxMessageContentSize() {
        return 8_000_000;
    }

    // this should account for encrypted messages and their headers.
    public int getMaxMessageLength() {
        return getMaxMessageContentSize() + 4096;
    }

    public int getMaxCacheReuse() {
        return 5;
    }

    public long getHandshakeTimeout() {
        return 2_500;
    }

    public long getMessageTimeout() {
        return 250L;
    }

    public long getMessageTimeout(long length) {
        long seconds = length / 125_000L;
        return seconds * 1000L + getMessageTimeout();
    }

    public int getMaxNetworkErrors() {
        return 25;
    }

    public int getMaxAllowedInboundConnections() {
        return 125;
    }

    public int getMaxAllowedOutboundConnections() {
        return 8;
    }

    public int getPort() {
        return isTestNet ? 5112 : 5110;
    }

    public double getMessageSpamThreshold() {
        return Math.PI;
    }

    public int getMaxCacheSize() {
        return 18_796_99;
    }

    public int getVersion() {
        return 1;
    }

    public boolean isVersionCompatible(int a, int b) {
        return a == b;
    }

    public long getServices() {
        return VersionInformation.Flags.AllServices;
    }

    public Address getFoundingAddresses() {
        return Address.fromRaw(new byte[20]);
    }

    public long getMaxFutureBlockTime() {
        return 144_000L;
    }

    public long getMaxTransactionUnconfirmedTime() {
        return 60_000L * 360;
    }

    public int getLoggingLevel() {
        return loggingLevel;
    }
}
