package io.adenium.network;

import io.adenium.core.Context;
import io.adenium.utils.ByteArray;
import io.adenium.utils.FileService;

import java.io.*;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class IpAddressList {
    private Map<ByteArray, NetAddress>  addresses;
    private FileService                 service;

    public IpAddressList(FileService service)
    {
        if (service.exists())
        {
            try {
                ObjectInputStream stream = new ObjectInputStream(new FileInputStream(service.file()));
                this.addresses = (Map<ByteArray, NetAddress>) stream.readObject();
                stream.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        else
        {
            this.addresses  = new HashMap<>();
        }

        this.service = service;
    }

    public void send(Node node) {
        Queue<NetAddress> addresses = new PriorityBlockingQueue<>(this.addresses.values());
        int sent = 0;

        while (!addresses.isEmpty()) {
            Set<NetAddress> list    = new LinkedHashSet<>();
            for (int i = 0; i < 1024; i ++) {
                list.add(addresses.poll());

                if (addresses.isEmpty()) {
                    break;
                }
            }

            node.sendMessage(new AddressList(Context.getInstance().getContextParams().getVersion(), list));
            if (++ sent == 1024) {
                return;
            }
        }
    }

    public void addAddress(NetAddress address)
    {
        addresses.put(ByteArray.wrap(address.getAddress().getAddress()), address);
    }

    public void removeAddress(NetAddress address)
    {
        addresses.remove(address);
    }

    public Queue<NetAddress> getAddresses()
    {
        return new PriorityQueue(addresses.values());
    }

    public void save() throws IOException {
        ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(service.file()));
        stream.writeObject(addresses);
        stream.flush();
        stream.close();
    }

    public NetAddress getAddress(InetAddress inetAddress) {
        return addresses.get(inetAddress.getAddress());
    }

    public void add(Set<NetAddress> addresses) {
        for (NetAddress address : addresses) {
            addAddress(address);
        }
    }
}
