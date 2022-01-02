package io.adenium.network;

import io.adenium.exceptions.AdeniumTimeoutException;
import io.adenium.network.messages.VersionMessage;
import io.adenium.core.Context;
import io.adenium.utils.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static io.adenium.utils.Logger.Levels.*;

public class Server implements Runnable {
    private ServerSocket    socket;
    private Set<Node>       connectedNodes;
    private NetAddress      netAddress;
    private ReentrantLock   mutex;
    private byte            nonce[];
    private long            upSince;

    public Server(Set<NetAddress> forceConnections) throws IOException {
        socket  = new ServerSocket();
        socket.bind(new InetSocketAddress(Context.getInstance().getContextParams().getPort()));
        upSince = System.currentTimeMillis();
        mutex   = new ReentrantLock();
        nonce   = new byte[20];

        // generate a nonce to know when we self connect
        new SecureRandom().nextBytes(nonce);

        // listen for incoming connections indefinitely
        Context.getInstance().getThreadPool().execute(this::listenForIncomingConnections);

        netAddress = Context.getInstance().getIpAddressList().getAddress(InetAddress.getLocalHost());
        if (netAddress == null)
        {
            netAddress = new NetAddress(InetAddress.getLocalHost(), Context.getInstance().getContextParams().getPort(), Context.getInstance().getContextParams().getServices());
            Context.getInstance().getIpAddressList().addAddress(netAddress);
        }

        Logger.alert("opened port '${port}' on '${address}'", AlertMessage, Context.getInstance().getContextParams().getPort(), netAddress.getAddress());

        connectedNodes = Collections.synchronizedSet(new LinkedHashSet<>());
        connectToNodes(forceConnections, Context.getInstance().getIpAddressList().getAddresses());
    }

    public boolean connectToNodes(Set<NetAddress> forceConnections, Queue<NetAddress> addresses) {
        Logger.alert("establishing outbound connections.", AlertMessage);
        int connections = 0;

        for (NetAddress address : forceConnections) {
            int i = connectedNodes.size();

            forceConnect(address);
            if (connectedNodes.size() == i) {
                Logger.error("failed to connect to ${address}", AlertMessage, address);
            } else {
                Logger.alert("connected to ${address}", AlertMessage, address);
            }
        }

        for (NetAddress address : addresses) {
            // prevent self connections.
            if (address.getAddress().isAnyLocalAddress() || address.getAddress().isLoopbackAddress()) {
                continue;
            }

            forceConnect(address);

            if (++connections == Context.getInstance().getContextParams().getMaxAllowedOutboundConnections()) {
                return true;
            }
        }

        return false;
    }

    private void forceConnect(InetAddress address, int port) {
        forceConnect(new NetAddress(address, port, 0));
    }

    private void forceConnect(NetAddress address) {
        try {
            Socket socket = new Socket();
            socket.bind(new InetSocketAddress(address.getAddress(), address.getPort()));

            Node node = new Node(socket);
            mutex.lock();
            try {
                connectedNodes.add(node);
            } finally {
                mutex.unlock();
            }

            node.sendMessage(new VersionMessage(
                    Context.getInstance().getContextParams().getVersion(),
                    new VersionInformation(
                            Context.getInstance().getContextParams().getVersion(),
                            VersionInformation.Flags.AllServices,
                            System.currentTimeMillis(),
                            getNetAddress(),
                            address,
                            Context.getInstance().getBlockChain().getHeight(),
                            nonce)));
        } catch (IOException e) {
        }
    }

    private void listenForIncomingConnections()
    {
        Logger.notify("listening for inbound connections.", NotificationMessage);
        Socket incoming = null;

        while (Context.getInstance().isRunning())
        {
            try {
                incoming = socket.accept();
                Logger.notify("received connection request from ${n}", AlertMessage, incoming.getSocketAddress());

                if (incoming != null) {
                    if (connectedNodes.size() < (Context.getInstance().getContextParams().getMaxAllowedInboundConnections() + Context.getInstance().getContextParams().getMaxAllowedOutboundConnections()))
                    {
                        Logger.alert("accepted connection request from ${n}", AlertMessage, incoming.getSocketAddress());
                        mutex.lock();
                        try {
                            connectedNodes.add(new Node(incoming));
                        } finally {
                            mutex.unlock();
                        }
                    }
                    else
                    {
                        incoming.close();
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    @Override
    public void run() {
        // we don't need to start checks right away
        long lastCheck = System.currentTimeMillis();
        long lastNotif = 0;

        while (Context.getInstance().isRunning())
        {
            long currentTime = System.currentTimeMillis();
            Set<Node> connectedNodes = getConnectedNodes();

            if (currentTime - lastNotif >= 24_000) {
                Logger.alert("server uptime: ${m}ms", Journaling,System.currentTimeMillis() - upSince);
                Logger.alert("connected: ${d}", Journaling, connectedNodes.size());
//                Logger.alert("list: ${s}", connectedNodes);

                lastNotif = System.currentTimeMillis();
            }

            if (currentTime - lastCheck >= 30_000)
            {
                runMaintenanceChecks();
                lastCheck = currentTime;
            }

            for (Node node : connectedNodes) {
                node.read();
            }

            for (Node node : connectedNodes)
            {
                if (!node.isConnected()) {
                    continue;
                }

                CachedMessage message = node.listenForMessage();

                if (message != null) {
                    if (!message.isSpam())
                    {
                        if (!node.hasPerformedHandshake() && !message.isHandshake())
                        {
                            // ignore message
                            continue;
                        }

                        message.getMessage().executePayload(this, node);
                    }
                }
            }

            for (Node node : connectedNodes)
            {
                node.flush();
            }
        }
    }

    private void runMaintenanceChecks() {
        mutex.lock();
        try {
            Iterator<Node> nodeIterator = connectedNodes.iterator();
            while (nodeIterator.hasNext()) {
                Node node = nodeIterator.next();
                boolean shouldDisconnect = false;
                boolean isSpammy = false;

                if (!node.hasPerformedHandshake() && node.timeSinceConnected() >= Context.getInstance().getContextParams().getHandshakeTimeout()) {
                    shouldDisconnect = true;
                }

                if (node.getTotalErrorCount() > Context.getInstance().getContextParams().getMaxNetworkErrors()) {
                    shouldDisconnect = true;
                }

                if (node.getSpamAverage() >= Context.getInstance().getContextParams().getMessageSpamThreshold()) {
                    shouldDisconnect = true;
                    isSpammy = true;
                }

                if (isSpammy) {
                    NetAddress address = Context.getInstance().getIpAddressList().getAddress(node.getInetAddress());
                    if (address != null) {
                        address.setSpamAverage(node.getSpamAverage());
                    }
                }

                if (shouldDisconnect) {
                    nodeIterator.remove();
                    try {
                        node.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (!shouldDisconnect && !isSpammy) {
                    if (node.getMessageCache().inboundCacheSize() > Context.getInstance().getContextParams().getMaxCacheSize()) {
                        NetAddress address = Context.getInstance().getIpAddressList().getAddress(node.getInetAddress());
                        if (address != null) {
                            address.setSpamAverage(node.getSpamAverage());
                        }
                        node.getMessageCache().clearInboundCache();
                    }

                    node.getMessageCache().clearOutboundCache();
                }
            }
        } finally {
            mutex.unlock();
        }
    }

    public void shutdown() {
        Logger.notify("closing connections.", AlertMessage);

        Iterator<Node> nodeIterator = connectedNodes.iterator();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.next();
            try {
                node.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Logger.notify("closed connections.", AlertMessage);
    }

    public NetAddress getNetAddress() {
        return netAddress;
    }

    public Message broadcastRequest(Message request) {
        return broadcastRequest(request, true);
    }

    public Message broadcastRequest(Message request, boolean fullResponse) {
        return broadcastRequest(request, fullResponse, Context.getInstance().getContextParams().getMessageTimeout());
    }

    public Message broadcastRequest(Message request, boolean fullResponse, long timeOut) {
        Set<Node> connectedNodes = getConnectedNodes();

        for (Node node : connectedNodes) {
            try {
                CheckedResponse response = node.getResponse(request, timeOut);
                if (response != null) {
                    if (response.noErrors()) {
                        return response.getMessage();
                    } else if (response.containsPartialResponse()) {
                        if (fullResponse) {
                            continue;
                        }

                        return response.getMessage();
                    }
                }
            } catch (AdeniumTimeoutException e) {
            }
        }

        return null;
    }

    public Set<Node> getConnectedNodes() {
        return new LinkedHashSet<>(connectedNodes);
    }

    public void broadcast(Message message, Node ...except) {
        Set<Node> connectedNodes = getConnectedNodes();

        if (except != null) {
            for (Node node : except) {
                connectedNodes.remove(node);
            }
        }

        for (Node node : connectedNodes) {
            node.sendMessage(message);
        }
    }

    public byte[] getNonce() {
        return nonce;
    }
}
