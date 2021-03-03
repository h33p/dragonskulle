/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * @author Oscar L SocketStore is for storing all the information about connected clients. It stores
 *     all of the sockets and can close and broadcast to all clients. It should be used alongside
 *     Server, it is the backbone of the server functions.
 */
public class SocketStore {
    /** The Server. */
    private ServerSocket mServer;
    /** The Store for all the sockets. */
    private final ArrayList<Socket> mStore;
    /** The timeout for accepting a client. */
    private static final int SO_TIMEOUT = 3000;

    /** Instantiates a new Socket store. */
    public SocketStore() {
        this.mStore = new ArrayList<>();
    }

    /**
     * Broadcast to all sockets.
     *
     * @param buf the buf to be broadcasted
     */
    public void broadcast(byte[] buf) {
        System.out.println("Broadcasting bytes");
        DataOutputStream dOut;
        for (Socket connection : mStore) {
            try {
                if (connection.isClosed()) {
                    System.out.println("Client socket output has closed");
                }
                System.out.println("--broadcasting to client " + connection.toString());
                dOut = new DataOutputStream(connection.getOutputStream());
                dOut.write(buf);
                System.out.println("--broadcast success");

            } catch (IOException e) {
                System.out.println("Error in broadcasting");
                System.out.println(e.toString());
            }
        }
    }

    /**
     * Init server.
     *
     * @param serverSocket the server socket
     */
    public void initServer(ServerSocket serverSocket) {
        try {
            this.mServer = serverSocket;
            this.mServer.setSoTimeout(SO_TIMEOUT);
            System.out.println("[SS] Server created @ " + serverSocket.getLocalSocketAddress());
        } catch (SocketException e) {
            System.out.println("Failed to create server");
            e.printStackTrace();
        }
    }

    /**
     * Add client to the store.
     *
     * @param sock the sock
     */
    public void addClient(Socket sock) {
        // TODO add check for invalid socket
        System.out.println("Adding client");
        System.out.println("Socket :" + sock.toString());
        this.mStore.add(sock);
    }

    /**
     * Gets server port.
     *
     * @return the server port
     */
    public int getServerPort() {
        return this.mServer.getLocalPort();
    }

    /** Closes the server. */
    public void close() {
        try {
            this.mServer.close();
        } catch (Exception ignored) {
        }

        this.mStore.clear();
        this.mServer = null;
    }

    /**
     * Shutdown socket.
     *
     * @param socket the socket
     */
    private void shutdownSocket(Socket socket) {
        try {
            socket.shutdownOutput();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets server ip.
     *
     * @return the server ip
     */
    public String getServerIp() {
        try {
            this.mServer.getInetAddress();
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Accept client socket.
     *
     * @return the socket
     */
    public Socket acceptClient() {
        try {
            return this.mServer.accept();
        } catch (IOException ignored) {
        }
        return null;
    }

    /**
     * Terminate client boolean.
     *
     * @param sock the sock
     * @return the boolean
     */
    public boolean terminateClient(Socket sock) {
        // if client connection failed, close the socket and remove
        this.shutdownSocket(sock);
        this.mStore.remove(sock);
        return true;
    }

    /**
     * Remove client.
     *
     * @param sock the sock
     */
    public void removeClient(Socket sock) {
        // only remove client from store as socket has closed
        if (!sock.isClosed()) {
            this.terminateClient(sock);
        } else {
            this.mStore.remove(sock);
        }
    }

    /**
     * Send bytes to client.
     *
     * @param client the client
     * @param response_bytes the response bytes
     */
    public void sendBytesToClient(ClientInstance client, byte[] response_bytes) {
        for (Socket sock : this.mStore) {
            if (sock.getPort() == client.PORT && sock.getInetAddress() == client.IP) {
                System.out.println("Sending bytes to client");
                try {
                    DataOutputStream dOut = new DataOutputStream(sock.getOutputStream());
                    dOut.write(response_bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
}
