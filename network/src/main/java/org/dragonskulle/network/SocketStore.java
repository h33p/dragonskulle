package org.dragonskulle.network;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;

public class SocketStore {
    private ServerSocket server;
    private final ArrayList<Socket> store;
    PrintWriter printWriter;
    final static int so_timeout = 3000;


    public SocketStore() {
        this.store = new ArrayList<>();
    }

    public void broadcast(String msg) {
        for (Socket connection : store) {
            try {
                if(connection.isClosed()){
                    System.out.println("Client socket output has closed");
                }
                printWriter = new PrintWriter(connection.getOutputStream(), true); //must be better way to do this
                printWriter.println(msg);
            } catch (IOException e) {
                System.out.println("Error in broadcasting");
                System.out.println(e.toString());
            }
        }
    }

    public void initServer(ServerSocket server_socket) {
        try {
            this.server = server_socket;
            this.server.setSoTimeout(so_timeout);
            System.out.println("[SS] Server created @ " + server_socket.getLocalSocketAddress());
        } catch (SocketException e) {
            System.out.println("Failed to create server");
            e.printStackTrace();
        }

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
        } catch (Exception ignored) {
        }


//        for (Socket sock : this.store) {
//            try {
//                sock.close();
//            } catch (Exception exception) {
//                exception.printStackTrace();
//            }
//        }

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
        } catch (IOException ignored) {
        }
        return null;
    }

    public boolean terminateClient(Socket sock) {
        //if client connection failed, close the socket and remove
        this.shutdownSocket(sock);
        this.store.remove(sock);
        return true;
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
