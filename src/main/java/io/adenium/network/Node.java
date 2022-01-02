package io.adenium.network;

import io.adenium.exceptions.WolkenException;
import io.adenium.exceptions.WolkenTimeoutException;
import io.adenium.network.messages.FailedToRespondMessage;
import io.adenium.utils.ByteArray;
import io.adenium.utils.Utils;
import org.json.JSONObject;
import io.adenium.core.Context;
import org.wolkenproject.utils.*;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class Node implements Runnable {
    private Socket                          socket;
    private ReentrantLock                   mutex;
    private Queue<Message>                  messages;
    private Queue<byte[]>                   messageQueue;
    private Map<ByteArray, ResponseMetadata>expectedResponse;
    private Map<ByteArray, Message>         respones;
    private MessageCache                    messageCache;
    private long                            firstConnected;
    private int                             errors;
    private byte                            readBuffer[];
//    private ByteBuffer                      buffer;
    private ByteArrayOutputStream           stream;
    private int                             currentMessageSize;
    private int                             receivedAddresses;
    private VersionInformation              versionMessage;
    private boolean                         isClosed;


//    public Node(String ip, int port) throws IOException {
//        this(new Socket(ip, port));
//    }

    public Node(Socket socket) throws IOException {
        this.socket         = socket;
        this.mutex          = new ReentrantLock();
        this.messageQueue   = new ConcurrentLinkedQueue<>();
        this.messages       = new ConcurrentLinkedQueue<>();
        this.messageCache   = new MessageCache();
        this.errors         = 0;
        this.stream         = null;
        this.currentMessageSize = -1;
        this.firstConnected = System.currentTimeMillis();
        this.respones       = Collections.synchronizedMap(new HashMap<>());
        this.readBuffer     = new byte[Context.getInstance().getContextParams().getBufferSize()];
        this.expectedResponse = new HashMap<>();
    }

    public void receiveResponse(Message message, byte origin[]) {
        mutex.lock();
        try{
            respones.put(ByteArray.wrap(origin), message);
        } finally {
            mutex.unlock();
        }
    }

    public CheckedResponse getResponse(Message message, long timeOut) throws WolkenTimeoutException {
        boolean shouldWait = false;
        byte id[] = message.getUniqueMessageIdentifier();

        mutex.lock();
        try {
            if (messageCache.shouldSend(message)) {
                messages.add(message);
                expectedResponse.put(ByteArray.wrap(id), message.getResponseMetadata());
                shouldWait = true;
            }
        } finally {
            mutex.unlock();
        }

        if (shouldWait) {
            long lastCheck = System.currentTimeMillis();

            while (System.currentTimeMillis() - lastCheck < timeOut) {
                if (hasResponse(id)) {
                    return getResponse(id);
                }
            }

            throw new WolkenTimeoutException("timed out while waiting for response");
        }

        return null;
    }

    private boolean hasResponse(byte[] uniqueMessageIdentifier) {
        mutex.lock();
        try{
            return respones.containsKey(ByteArray.wrap(uniqueMessageIdentifier));
        } finally {
            mutex.unlock();
        }
    }

    private CheckedResponse getResponse(byte[] uniqueMessageIdentifier) {
        mutex.lock();
        try{
            Message response            = respones.get(ByteArray.wrap(uniqueMessageIdentifier));
            ResponseMetadata metadata   = expectedResponse.get(ByteArray.wrap(uniqueMessageIdentifier));

            // internal error, we may return null
            if (response == null) {
                return null;
            }

            // may return without any issues
            if (response.getSerialNumber() == Context.getInstance().getSerialFactory().getSerialNumber(FailedToRespondMessage.class)) {
                return null;
            }

            int flags = metadata.getResponseBits(response);

            // check that the response is appropriate
            if ((flags & ResponseMetadata.ValidationBits.InvalidType) == ResponseMetadata.ValidationBits.InvalidType) {
                errors ++;
                return null;
            }

            // check that the response is appropriate
            if ((flags & ResponseMetadata.ValidationBits.SpamfulResponse) == ResponseMetadata.ValidationBits.SpamfulResponse) {
                errors += Context.getInstance().getContextParams().getMaxNetworkErrors();
                messageCache.increaseSpamAverage(Context.getInstance().getContextParams().getMessageSpamThreshold());
                return null;
            }

            return new CheckedResponse(response, flags);
        } finally {
            respones.remove(ByteArray.wrap(uniqueMessageIdentifier));
            expectedResponse.remove(ByteArray.wrap(uniqueMessageIdentifier));
            mutex.unlock();
        }
    }

    /*
        Sends a message only if it was not sent before.
     */
    public void sendMessage(Message message) {
        mutex.lock();
        try{
            if (messageCache.shouldSend(message))
            {
                messages.add(message);
            }
        } finally {
            mutex.unlock();
        }
    }

    /*
        Forcefully sends a message even if it was sent before.
     */
    public void forceSendMessage(Message message) {
        mutex.lock();
        try{
            messages.add(message);
        } finally {
            mutex.unlock();
        }
    }

    public void read() {
        mutex.lock();

        try {
            if (!socket.isOpen()) {
                return;
            }

            if (stream != null && stream.size() == currentMessageSize) {
                stream.flush();
                stream.close();

                // queue the message for processing.
                finish(stream);

                stream = null;
                currentMessageSize = -1;
            }

            if (stream == null) {
                stream              = new ByteArrayOutputStream();
                currentMessageSize  = -1;
            }

            // read message size
            if (currentMessageSize < 0) {
                byte smallBuffer[] = new byte[4];
                socket.read(smallBuffer);

                currentMessageSize = Utils.makeInt(smallBuffer);

                if (currentMessageSize <= 0) {
                    errors ++;
                    messageCache.increaseSpamAverage(0.2);
                    // queue the message for processing.
                    finish(stream);
                    currentMessageSize = -1;
                }

                if (currentMessageSize > Context.getInstance().getContextParams().getMaxMessageLength()) {
                    errors += Context.getInstance().getContextParams().getMaxNetworkErrors();
                    stream = null;
                    currentMessageSize = -1;
                    throw new WolkenException("message content exceeds the maximum size allowed by the protocol.");
                }
            }

            // blocking read (5ms max block)
            int read = socket.read(readBuffer);

            if (read > 0) {
                stream.write(readBuffer, 0, read);
            }

            if (stream != null && stream.size() == currentMessageSize) {
                // queue the message for processing.
                finish(stream);

                stream = null;
                currentMessageSize = -1;
            }
        } catch (WolkenException | IOException e) {
            e.printStackTrace();
            errors ++;
            if (stream != null) {
                stream = null;
                currentMessageSize = -1;
            }
        } finally {
            mutex.unlock();
        }
    }

    private void finish(ByteArrayOutputStream stream) {
        if (stream.size() > 0) {
            messageQueue.add(stream.toByteArray());
        }
    }

    /*
        Listens for any incoming messages.
     */
    public CachedMessage listenForMessage() {
        mutex.lock();
        try {
            if (messageQueue.isEmpty()) {
                return null;
            }

            byte msg[] = messageQueue.poll();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(msg);
            Message message = Context.getInstance().getSerialFactory().fromStream(inputStream);
            inputStream.close();

            return checkSpam(message);
        } catch (IOException | WolkenException e) {
            errors++;
            e.printStackTrace();
            return null;
        } finally {
            mutex.unlock();
        }
    }

    /*
        Caches the message and keeps track of how many times it was received.
     */
    private CachedMessage checkSpam(Message message) {
        return new CachedMessage(message, messageCache.cacheReceivedMessage(message) >= Context.getInstance().getContextParams().getMessageSpamThreshold());
    }

    /*
        Send all queued messages.
     */
    public void flush()
    {
        mutex.lock();
        try{
            if (!socket.isOpen()) {
                return;
            }

            while (!messages.isEmpty() && socket.isOpen()) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                Message message = messages.poll();
                message.serialize(outputStream);
                outputStream.flush();
                outputStream.close();

                byte msg[] = outputStream.toByteArray();

                // write the length of the message
                socket.write(Utils.takeApart(msg.length));

                // write the actual  message
                socket.write(msg);

                // notify the message that it was sent
                message.onSend(this);
            }
        } catch (IOException | WolkenException e) {
            e.printStackTrace();
        } finally {
            mutex.unlock();
        }
    }

    public void close() throws IOException {
        isClosed = true;
        socket.close();
    }

    public int getTotalErrorCount() {
        return errors;
    }

    public double getSpamAverage() {
        return messageCache.getAverageSpam();
    }

    public InetAddress getInetAddress() {
        return ((InetSocketAddress) socket.getSocketAddress()).getAddress();
    }

    public MessageCache getMessageCache()
    {
        return messageCache;
    }

    public void setVersionInfo(VersionInformation versionMessage) {
        this.versionMessage = versionMessage;
        getNetAddress().setServices(versionMessage.getServices());
    }

    public boolean hasPerformedHandshake()
    {
        return versionMessage != null;
    }

    public NetAddress getNetAddress() {
        NetAddress address = Context.getInstance().getIpAddressList().getAddress(getInetAddress());
        if (address == null)
        {
            long services = 0;
            if (versionMessage != null)
            {
                services = versionMessage.getServices();
            }

            address = new NetAddress(getInetAddress(), getPort(), services);
            Context.getInstance().getIpAddressList().addAddress(address);
        }

        return address;
    }

    public int getPort() {
        return  ((InetSocketAddress)socket.getSocketAddress()).getPort();
    }

    public long timeSinceConnected()
    {
        return System.currentTimeMillis() - firstConnected;
    }

    @Override
    public void run() {
        while (Context.getInstance().isRunning()) {
        }
    }

    public void incrementReceivedAddresses() {
        receivedAddresses ++;

        if (receivedAddresses > 1024) {
            errors += Context.getInstance().getContextParams().getMaxNetworkErrors();
        }
    }

    @Override
    public String toString() {
        return "Node{" +
                "messages=" + messages.size() +
                ", messageQueue=" + messageQueue.size() +
                ", expectedResponse=" + expectedResponse.size() +
                ", respones=" + respones.size() +
                ", messageCache=" + messageCache.inboundCacheSize() + messageCache.outboundCacheSize() +
                ", firstConnected=" + firstConnected +
                ", errors=" + errors +
                ", versionMessage=" + versionMessage +
                ", isClosed=" + isClosed +
                '}';
    }

    public VersionInformation getVersionInfo() {
        return versionMessage;
    }

    public void increaseErrors(int i) {
        errors += i;
    }

    public boolean isConnected() {
        return socket.isOpen();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        if (versionMessage != null) {
            json.put("versionmsg", versionMessage.toJson());
        } else {
            json.put("versionmsg", "null");
        }
        json.put("inetaddress", getNetAddress().toJson());
        json.put("messagequeue", messages.size());
        json.put("responsequeue", expectedResponse.size());
        json.put("responses", respones.size());
        json.put("firstconnected", Utils.jsonDate(firstConnected));
        json.put("closed", isClosed);

        return json;
    }
}
