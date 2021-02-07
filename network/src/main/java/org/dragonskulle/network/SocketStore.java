package org.dragonskulle.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class SocketStore {
    private ServerSocket server;
    private final ArrayList<Socket> store;

    public SocketStore() {
        this.store = new ArrayList<>();
    }

    public void initServer(ServerSocket server_socket) {
        this.server = server_socket;
        System.out.println("[SS] Server created @ "+ server_socket.getLocalSocketAddress());

    }

    public void addClient(Socket sock) {
        //TODO add check for invalid socket
        System.out.println("Adding client");
        System.out.println("Socket :" + sock.toString());
        this.store.add(sock);
    }


    public int getServerPort() {
        return this.server.getLocalPort();
    }

    public void close() {

        try {
            this.server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Socket sock : this.store) {
            try {
                sock.close();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        this.store.clear();
        this.server = null;
    }

    private void shutdownSocket(Socket socket) {
        try {
            socket.shutdownOutput();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getServerIp() {
        try {
            this.server.getInetAddress();
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Socket acceptClient() {
        try {
            return this.server.accept();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void terminateClient(Socket sock) {
        //if client connection failed, close the socket and remove
        this.shutdownSocket(sock);
        this.store.remove(sock);
    }

    public void removeClient(Socket sock) {
        //only remove client from store as socket has closed
        if (!sock.isClosed()) {
            this.terminateClient(sock);
        } else {
            this.store.remove(sock);
        }
    }
}
