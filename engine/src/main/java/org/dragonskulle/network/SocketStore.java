/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Oscar L SocketStore is for storing all the information about connected clients. It stores
 *     all of the sockets and can close and broadcast to all clients. It should be used alongside
 *     Server, it is the backbone of the server functions.
 */
public class SocketStore {
    private static final Logger mLogger = Logger.getLogger(SocketStore.class.getName());

    /** The Server. */
    private ServerSocket mServer;
    /** The Store for all the sockets. */
    private final ArrayList<Socket> mStore;
    /** The timeout for accepting a client. */
    private static final int SO_TIMEOUT = 400;

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
        DataOutputStream dOut;
        List<Socket> asyncList = Collections.synchronizedList(this.mStore);
        for (Socket connection : asyncList) {
            try {
                if (connection.isClosed()) {
                    mLogger.fine("Client socket output has closed");
                }
                dOut = new DataOutputStream(connection.getOutputStream());
                dOut.write(buf);

            } catch (IOException e) {
                mLogger.fine("Error in broadcasting");
                mLogger.fine(e.toString());
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
            mLogger.fine("[SS] Server created @ " + serverSocket.getLocalSocketAddress());
        } catch (SocketException e) {
            mLogger.fine("Failed to create server");
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
        mLogger.fine("Adding client");
        mLogger.fine("Socket :" + sock.toString());
        Collections.synchronizedList(this.mStore).add(sock);
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
            if (this.mServer != null) {
                this.mServer.close();
            }
        } catch (Exception ignored) {
        }

        Collections.synchronizedList(this.mStore).clear();
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
        return Collections.synchronizedList(this.mStore).remove(sock);
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
            Collections.synchronizedList(this.mStore).remove(sock);
        }
    }

    /**
     * Send bytes to client.
     *
     * @param client the client
     * @param response_bytes the response bytes
     */
    public void sendBytesToClient(ClientInstance client, byte[] response_bytes) {
        List<Socket> asyncList = Collections.synchronizedList(this.mStore);

        for (Socket sock : asyncList) {
            if (sock.getPort() == client.PORT && sock.getInetAddress() == client.IP) {
                mLogger.fine("Sending bytes to client");
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
