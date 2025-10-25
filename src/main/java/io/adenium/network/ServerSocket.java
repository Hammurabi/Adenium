package io.adenium.network;

import java.io.IOException;
import java.net.SocketAddress;

public class ServerSocket {
    private java.net.ServerSocket   socket;
    private boolean                 isOpen;

    public ServerSocket() throws IOException {
        socket = new java.net.ServerSocket();
        isOpen = false;
        socket.setSoTimeout(5);
    }

    public void bind(SocketAddress address) throws IOException {
        socket.bind(address);
        isOpen = true;
    }

    public Socket accept() throws IOException {
        java.net.Socket socket = this.socket.accept();

        if (socket != null) {
            return new Socket(socket);
        }

        return null;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void close() throws IOException {
        socket.close();
    }
}
