package io.adenium.network;

import io.adenium.utils.ByteArray;

import java.util.HashMap;
import java.util.Map;

public class MessageCache {
    private Map<ByteArray, Integer> receivedMessages;
    private Map<ByteArray, Integer> sentMessages;
    private double                  spamAverage;

    public MessageCache() {
        receivedMessages    = new HashMap<>();
        sentMessages        = new HashMap<>();
        spamAverage         = 0;
    }

    public int cacheReceivedMessage(Message message) {
        ByteArray messageId = ByteArray.wrap(message.getUniqueMessageIdentifier());

        if (receivedMessages.containsKey(messageId)) {
            int timesReceived = receivedMessages.get(messageId);
            receivedMessages.put(messageId, receivedMessages.get(messageId) + 1);
            return timesReceived;
        } else {
            receivedMessages.put(messageId, 1);
        }

        return 0;
    }

    private int numTimesReceived(Message message) {
        ByteArray messageId = ByteArray.wrap(message.getUniqueMessageIdentifier());

        if (receivedMessages.containsKey(messageId)) {
            return receivedMessages.get(messageId);
        }

        return 0;
    }

    public boolean shouldSend(Message message) {
        ByteArray messageId = ByteArray.wrap(message.getUniqueMessageIdentifier());
        int timesSent = 0;

        if (sentMessages.containsKey(messageId)) {
            timesSent = sentMessages.get(messageId);
            sentMessages.put(messageId, timesSent + 1);
        } else {
            sentMessages.put(messageId, 1);
        }

        return timesSent < 4;
    }

    public double getAverageSpam() {
        double numTimes = 0;
        for (Integer integer : receivedMessages.values())
        {
            numTimes += integer.doubleValue();
        }

        return ((numTimes / receivedMessages.size()) - 1.0) + spamAverage;
    }

    public void clearOutboundCache()
    {
        sentMessages.clear();
    }

    public void clearInboundCache()
    {
        receivedMessages.clear();
    }

    /*
        Return an estimate of the memory consumption of the inbound cache.
     */
    public int inboundCacheSize()
    {
        return receivedMessages.size() * 40;
    }

    /*
        Return an estimate of the memory consumption of the outbound cache.
     */
    public int outboundCacheSize()
    {
        return sentMessages.size() * 40;
    }

    public void increaseSpamAverage(double spam) {
        spamAverage += spam;
    }
}