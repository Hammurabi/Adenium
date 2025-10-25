package io.adenium.network;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class Socket {
    private java.net.Socket socket;
    private boolean         isOpen;
    private boolean         isClosed;

    public Socket() {
        socket      = new java.net.Socket();
        isOpen      = false;
        isClosed    = false;
    }

    protected Socket(java.net.Socket socket) throws SocketException {
        this.socket = socket;
        socket.setSoTimeout(5);
        isOpen      = true;
        isClosed    = false;
    }

    public void bind(SocketAddress socketAddress) throws IOException {
        socket.connect(socketAddress);
        socket.setSoTimeout(5);
        isOpen      = true;
        isClosed    = false;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean isClosed() {
        return isClosed;
    }

    // reads data into the buffer
    // blocks for a maximum of 5ms
    // returns length of the data
    // or -1 if nothing is available
    public int read(byte buffer[]) throws IOException {
        try {
            return socket.getInputStream().read(buffer);
        } catch (SocketTimeoutException e) {
            return -1;
        }
    }

    public void write(byte buffer[]) throws IOException {
        write(buffer, 0, buffer.length);
    }

    public void write(byte buffer[], int offset, int length) throws IOException {
        socket.getOutputStream().write(buffer, offset, length);
    }

    public void close() throws IOException {
        isClosed    = true;
        isOpen      = false;
        socket.close();
    }

    public SocketAddress getSocketAddress() {
        return socket.getRemoteSocketAddress();
    }
}
