/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;
// originally based on
// https://github.com/TheDudeFromCI/WraithEngine/tree/5397e2cfd75c257e4d96d0fd6414e302ab22a69c/WraithEngine/src/wraith/library/Multiplayer
// later rewritten

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import lombok.experimental.Accessors;

/**
 * The type Server.
 *
 * @author Oscar L
 * @author Aurimas Blažulionis
 *     <p>This is the main Server Class, it handles setup and stores all client connections. It can
 *     broadcast messages to every client and receive from individual clients.
 */
@Accessors(prefix = "m")
public class Server {
    private static final Logger mLogger = Logger.getLogger(Server.class.getName());
    private static final int MAX_CLIENTS = 128;

    /** The timeout for accepting a client. */
    private static final int SO_TIMEOUT = 400;

    /** The Port. */
    private int mPort;
    /** The Server listener. */
    private IServerListener mServerListener;

    private final ServerSocket mServerSocket;
    /** The Server thread. */
    private Thread mServerThread;
    /** The Server runner. */
    private ServerRunner mServerRunner;

    /** Array of clients. Indexed by their network ID */
    private final Map<Integer, ServerClient> mClients = new TreeMap<>();

    /**
     * Accepted incoming clients. They have not been allocated any IDs yet, just waiting to be
     * accepted by the main thread.
     */
    private final ConcurrentLinkedQueue<Socket> mPendingClients = new ConcurrentLinkedQueue<>();
    /**
     * Total client count. This includes all mClients, and not yet allocated PendingConnectedClients
     */
    private int mClientCount = 0;
    /** Network client ID counter. This is so as to ensure that every client has unique ID */
    private final AtomicInteger mClientIDCounter = new AtomicInteger(0);
    /**
     * Pending clients. Sockets for these clients have already been set up, they just need to be
     * linked up by the main thread
     */
    private final ConcurrentLinkedQueue<ServerClient> mPendingConnectedClients =
            new ConcurrentLinkedQueue<>();
    /**
     * Pending disconnected clients. These are the clients main thread is supposed to remove on the
     * next update.
     */
    private final ConcurrentLinkedQueue<ServerClient> mPendingDisconnectedClients =
            new ConcurrentLinkedQueue<>();

    /**
     * Instantiates a new Server. Scene linking is required once the scene is created.
     *
     * @param port the port
     * @param listener the listener
     */
    public Server(int port, IServerListener listener) throws IOException {
        mLogger.fine("[S] Setting up server");
        mServerListener = listener;

        mServerSocket =
                new ServerSocket(port, 0, InetAddress.getByName(null)); // sets up on localhost
        mServerSocket.setSoTimeout(SO_TIMEOUT);

        if (this.mPort == 0) {
            this.mPort = mServerSocket.getLocalPort();
        } else {
            this.mPort = port;
        }
        mServerRunner = new ServerRunner();
        mServerThread = new Thread(this.mServerRunner);
        mServerThread.setDaemon(true);
        mServerThread.setName("Server");
        mLogger.fine("[S] Starting server");
        mServerThread.start();
    }

    public Collection<ServerClient> getClients() {
        return mClients.values();
    }

    /**
     * Update client lists
     *
     * <p>This method will cleanup closed connections, and accept new clients in.
     */
    public void updateClientList() {
        // First, cleanup any disconnected clients
        ServerClient c;
        while ((c = mPendingDisconnectedClients.poll()) != null) removeClient(c);

        byte[] netID = {-1};

        // Secondly accept all clients that already connected
        while ((c = mPendingConnectedClients.poll()) != null) {
            mClients.put(c.getNetworkID(), c);
            netID[0] = (byte) c.getNetworkID();
            c.sendBytes(netID);
            mServerListener.clientActivated(c);
        }

        // Now accept new socket connections
        Socket s;
        while (mClientCount < MAX_CLIENTS && (s = mPendingClients.poll()) != null) {
            new ServerClient(s, mServerListener).startThread();
            mClientCount++;
        }
    }

    /**
     * Process requests on the clients
     *
     * @param clientRequests maximum number of requests to process per client
     * @return total number of requests processed
     */
    public int processClientRequests(int clientRequests) {
        int cnt = 0;

        for (ServerClient c : mClients.values()) cnt += c.processRequests(clientRequests);

        return cnt;
    }

    /**
     * Add connected client to pending client list
     *
     * <p>This method will be called from outside the main thread. We will place the client to a
     * pending list that will be processed on the next network update. And then, the client will be
     * fully spawned in.
     *
     * @param client client to connect
     * @return its allocated network client ID
     */
    public int addConnectedClient(ServerClient client) {
        int id = mClientIDCounter.getAndIncrement();
        client.setNetworkID(id);
        mPendingConnectedClients.add(client);
        return id;
    }

    /**
     * Thread safe client disconnect event
     *
     * @param client client to mark for removal
     */
    public void onClientDisconnect(ServerClient client) {
        mPendingDisconnectedClients.add(client);
    }

    private boolean removeClient(ServerClient c) {
        ServerClient mapClient = mClients.remove(c.getNetworkID());

        if (mapClient != c) {
            mLogger.warning("Illegal client passed!");
        } else if (mapClient != null) {
            mapClient.closeSocket();
            mapClient.joinThread();
            mClientCount--;
            return true;
        }

        return false;
    }

    /** Dispose. */
    public void dispose() {
        for (ServerClient c : mClients.values()) c.closeSocket();

        if (mServerRunner != null) {
            mServerRunner.cancel();

            try {
                this.mServerThread.join();
            } catch (InterruptedException e) {
                mLogger.warning("Server thread was interrupted!");
                e.printStackTrace();
            }

            mServerRunner = null;
        }

        if (mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (IOException e) {

            }
        }

        Socket s;
        while ((s = mPendingClients.poll()) != null) {
            try {
                s.shutdownOutput();
                s.close();
                mClientCount--;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (ServerClient c : mClients.values()) c.joinThread();

        mClientCount -= mClients.size();

        mClients.clear();

        if (mClientCount != 0) {
            mLogger.severe("Client count non-zero on disposal! Current count: " + mClientCount);
        }
    }

    /**
     * ServerRunner is the thread which constantly checks for new client requests, if a client has
     * requested a socket, it will provide it a thread to communicate on and accept the socket.
     * SO_TIMEOUT is set so that sockets.acceptClient won't block the joining of the thread
     * indefinitely.
     */
    private class ServerRunner implements Runnable {
        /** True if the server is open. */
        volatile boolean mOpen = true;

        @Override
        public void run() {
            while (mOpen && !Thread.currentThread().isInterrupted()) {
                Socket clientSocket = null;

                try {
                    clientSocket = mServerSocket.accept();
                } catch (IOException __) {
                }

                if (clientSocket != null) {
                    mPendingClients.add(clientSocket);
                }
            }
        }

        /** Cancel. */
        public void cancel() {
            this.mOpen = false;
        }
    }
}
