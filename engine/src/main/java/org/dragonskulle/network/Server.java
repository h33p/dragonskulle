/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;
// based on
// https://github.com/TheDudeFromCI/WraithEngine/tree/5397e2cfd75c257e4d96d0fd6414e302ab22a69c/WraithEngine/src/wraith/library/Multiplayer

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.dragonskulle.network.components.Capital;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.NetworkableComponent;

/**
 * @author Oscar L
 *     <p>This is the main Server Class, it handles setup and stores all client connections. It can
 *     broadcast messages to every client and receive from individual clients.
 */
public class Server {

    private boolean mAutoProcessMessages = false;
    /** The Port. */
    private int mPort;
    /** The Server listener. */
    private ServerListener mServerListener;
    /** The socket connections to all clients. */
    private final SocketStore mSockets = new SocketStore();
    /** The Server thread. */
    private Thread mServerThread;
    /** The Server runner. */
    private ServerRunner mServerRunner;
    /** The game instance for the server. */
    private ServerGameInstance mGame;
    /**
     * The Network objects - this can be moved to game instance but no point until game has been
     * merged in.
     */
    public final ArrayList<NetworkObject> networkObjects = new ArrayList<>();

    private final ListenableQueue<Request> mRequests = new ListenableQueue<>(new LinkedList<>());
    private final AtomicInteger mNetworkObjectCounter = new AtomicInteger(0);

    /**
     * Instantiates a new Server.
     *
     * @param port the port
     * @param listener the listener
     */
    public Server(int port, ServerListener listener) {
        System.out.println("[S] Setting up server");
        mServerListener = listener;
        try {
            ServerSocket server_sock =
                    new ServerSocket(port, 0, InetAddress.getByName(null)); // sets up on localhost
            mSockets.initServer(server_sock);
            if (this.mPort == 0) {
                this.mPort = mSockets.getServerPort();
            } else {
                this.mPort = port;
            }
            this.createGame();

            mServerRunner = new ServerRunner();
            mServerThread = new Thread(this.mServerRunner);
            mServerThread.setDaemon(true);
            mServerThread.setName("Server");
            System.out.println("[S] Starting server");
            mServerThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Instantiates a new Server in debug mode
     *
     * @param port the port
     * @param listener the listener
     * @param debug sets debug mode
     */
    public Server(int port, ServerListener listener, boolean debug) {
        System.out.println("[S] Setting up server in debug mode");
        this.mAutoProcessMessages = debug;
        mServerListener = listener;
        try {
            ServerSocket server_sock =
                    new ServerSocket(port, 0, InetAddress.getByName(null)); // sets up on localhost
            mSockets.initServer(server_sock);
            if (this.mPort == 0) {
                this.mPort = mSockets.getServerPort();
            } else {
                this.mPort = port;
            }
            this.createGame();

            mServerRunner = new ServerRunner();
            mServerThread = new Thread(this.mServerRunner);
            mServerThread.setDaemon(true);
            mServerThread.setName("Server");
            System.out.println("[S] Starting server");
            mServerThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Execute bytes on the server.
     *
     * @param messageType the message type
     * @param payload the payload
     * @param sendBytesToClient the socket of the requesting client, to be called if a communication
     *     directly to the client is needed
     */
    public static void executeBytes(
            byte messageType, byte[] payload, SendBytesToClientCurry sendBytesToClient) {
        byte[] message;
        switch (messageType) {
            case (byte) 22:
                message = NetworkMessage.build((byte) 20, "TOSPAWN".getBytes());
                sendBytesToClient.send(message);
                break;
            default:
                System.out.println("Should implement spawn and create building ____");
                message = NetworkMessage.build((byte) 20, "TOSPAWN".getBytes());
                sendBytesToClient.send(message);
                break;
        }
    }

    /** Dispose. */
    public void dispose() {
        try {
            this.mServerRunner.cancel();
            this.mServerThread.join();
            this.mSockets.close();
            if (mServerListener != null) {
                this.mServerListener.serverClosed();
                this.mServerListener = null;
            }
        } catch (InterruptedException e) {
            System.out.println("Error disposing");
            System.out.println(e.toString());
        }
    }

    /** Create game. */
    public void createGame() {
        this.mGame = new ServerGameInstance();
    }

    /**
     * Find networkable component by id.
     *
     * @param componentId the component id
     * @return the networkable component
     */
    public NetworkableComponent findComponent(int componentId) {
        NetworkableComponent found = null;
        for (NetworkObject e : this.networkObjects) {
            found = e.findComponent(componentId);
            if (found != null) {
                break;
            }
        }
        return found;
    }

    /**
     * ServerRunner is the thread which constantly checks for new client requests, if a client has
     * requested a socket, it will provide it a thread to communicate on and accept the socket.
     * SO_TIMEOUT is set so that sockets.acceptClient won't block the joining of the thread
     * indefinitely.
     */
    private class ServerRunner implements Runnable {
        /** The Open. */
        volatile boolean mOpen = true;

        private final Timer mProcessTimer = new Timer();

        @Override
        public void run() {
            while (mOpen && !Thread.currentThread().isInterrupted()) {
                if (mGame.isSetup()) {
                    if (!mAutoProcessMessages) {
                        mProcessTimer.schedule(new FixedBroadCastUpdateSchedule(), 0, 500);
                    }
                    Socket clientSocket = mSockets.acceptClient();
                    if (clientSocket != null) {
                        Thread clientThread = new Thread(clientRunner(clientSocket));
                        clientThread.setDaemon(true);
                        clientThread.setName("Client " + clientSocket.getInetAddress().toString());
                        clientThread.start();
                    }
                }
            }
        }

        /** Cancel. */
        public void cancel() {
            this.mOpen = false;
            this.mProcessTimer.cancel();
        }
    }

    private class FixedBroadCastUpdateSchedule extends TimerTask {
        public void run() {
            processRequests();
        }
    }

    /**
     * THe Client Runner is the thread given to each client to handle its own socket. Commands are
     * read from the input stream. It will pass all commands to the correct handler function. {@link
     * org.dragonskulle.network.ServerListener}**
     *
     * @param sock the sock
     * @return runnable runnable
     */
    private Runnable clientRunner(Socket sock) {
        if (sock == null) {
            return () -> {};
        }
        return () -> {
            try {
                System.out.println("Spawning client thread");
                boolean connected;
                int hasBytes = 0;
                final int MAX_TRANSMISSION_SIZE = NetworkConfig.MAX_TRANSMISSION_SIZE;
                byte[] bArray; // max flatbuffer size
                byte[] terminateBytes = new byte[MAX_TRANSMISSION_SIZE]; // max flatbuffer size
                this.mSockets.addClient(sock);

                BufferedInputStream bIn = new BufferedInputStream(sock.getInputStream());
                PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                // create client as object
                ClientInstance client = new ClientInstance(sock.getInetAddress(), sock.getPort());
                mServerListener.clientConnected(client, out);
                connected = sock.isConnected();

                if (connected) {
                    // Spawn network object for the map and capital
                    NetworkObject networkObject = new NetworkObject(this.allocateId());
                    networkObject.spawnMap(
                            this.mGame.cloneMap(),
                            (message) -> this.mSockets.sendBytesToClient(client, message));
                    int capitalId =
                            networkObject.spawnCapital(
                                    networkObject.getId(), this.mSockets::broadcast);
                    this.networkObjects.add(networkObject);

                    // Simulation of calling fixed update;
                    Timer timer = new Timer();
                    int begin = 0;
                    int timeInterval = 1000;
                    FixedUpdateSimulation fixedUpdate = this::fixedBroadcastUpdate;
                    timer.schedule(
                            new TimerTask() {
                                int counter = 0;

                                @Override
                                public void run() {
                                    fixedUpdate.call();
                                    counter++;
                                    if (counter >= 20) {
                                        timer.cancel();
                                    }
                                }
                            },
                            begin,
                            timeInterval);

                    //                    set bool of capitol at some point in the future
                    timer.schedule(
                            new TimerTask() {
                                @Override
                                public void run() {
                                    Capital networkCapital =
                                            (Capital) getNetworkableChild(networkObject, capitalId);
                                    if (networkCapital != null) {
                                        networkCapital.setBooleanSyncMe(true);
                                    }
                                }
                            },
                            3000);

                    // set string of capitol at some point in the future
                    timer.schedule(
                            new TimerTask() {
                                @Override
                                public void run() {
                                    Capital networkCapital =
                                            (Capital) getNetworkableChild(networkObject, capitalId);
                                    if (networkCapital != null) {
                                        networkCapital.setStringSyncMeAlso("Goodbye World");
                                    }
                                }
                            },
                            3000);
                }
                while (connected) {
                    try {
                        bArray = NetworkMessage.readMessageFromStream(bIn);
                        if (bArray.length != 0) {
                            if (Arrays.equals(bArray, terminateBytes)) {
                                this.mSockets.terminateClient(sock); // close and remove
                                mServerListener.clientDisconnected(client);
                                connected = false;
                            } else {
                                queueRequest(client, bArray);
                                //                                processBytes(client, bArray);

                            }
                        }
                    } catch (IOException e) {
                        this.mSockets.terminateClient(sock); // close and remove
                        mServerListener.clientDisconnected(client);
                        connected = false;
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        };
    }

    private void queueRequest(ClientInstance client, byte[] bArray) {
        this.mRequests.add(new Request(client, bArray));
    }

    public void processRequests() {
        if (!this.mRequests.isEmpty()) {
            for (int i = 0; i < this.mRequests.size(); i++) {
                Request request = this.mRequests.poll();
                if (request != null) {
                    processBytes(request.client, request.bytes);
                }
            }
        }
    }

    public boolean hasRequests() {
        return !this.mRequests.isEmpty();
    }

    public void processSingleRequest() {
        if (!this.mRequests.isEmpty()) {
            Request request = this.mRequests.poll();
            if (request != null) {
                processBytes(request.client, request.bytes);
            }
        }
    }

    public void clearPendingRequests() {
        this.mRequests.clear();
    }

    /**
     * Gets networkable child.
     *
     * @param networkObject the network object
     * @param id the id
     * @return the networkable child
     */
    private NetworkableComponent getNetworkableChild(NetworkObject networkObject, int id) {
        final NetworkObject serverNetworkObject =
                this.networkObjects.get(this.networkObjects.indexOf(networkObject));
        return serverNetworkObject.get(id);
    }

    /**
     * Process bytes.
     *
     * @param client the client
     * @param bytes the bytes
     */
    private void processBytes(ClientInstance client, byte[] bytes) {

        mServerListener.receivedBytes(client, bytes);
        try {
            parseBytes(client, bytes);
        } catch (DecodingException e) {
            System.out.println(e.getMessage());
            System.out.println(new String(bytes, StandardCharsets.UTF_8));
        }
    }

    /**
     * Parse bytes.
     *
     * @param client the client
     * @param bytes the bytes
     * @throws DecodingException Thrown if there was any issue with the bytes
     */
    private void parseBytes(ClientInstance client, byte[] bytes) throws DecodingException {
        System.out.println("bytes parsing");
        try {
            NetworkMessage.parse(
                    bytes, (parsedBytes) -> this.mSockets.sendBytesToClient(client, parsedBytes));
        } catch (Exception e) {
            System.out.println("Error in parseBytes");
            e.printStackTrace();
            throw new DecodingException("Message is not of valid type");
        }
    }

    /** The interface Send bytes to client curry. */
    public interface SendBytesToClientCurry {
        /**
         * Send.
         *
         * @param bytes the bytes
         */
        void send(byte[] bytes);
    }

    /** The interface Fixed update simulation. */
    private interface FixedUpdateSimulation {
        /** Call. */
        void call();
    }

    /** Fixed broadcast update. */
    public void fixedBroadcastUpdate() {
        //        System.out.println("fixed broadcast update");
        processRequests();
        for (NetworkObject networkObject : this.networkObjects) {
            networkObject.broadcastUpdate(this.mSockets::broadcast);
        }
    }

    private class Request {
        public final ClientInstance client;
        public final byte[] bytes;

        Request(ClientInstance client, byte[] bytes) {
            this.client = client;
            this.bytes = bytes;
        }
    }

    private int allocateId() {
        return mNetworkObjectCounter.getAndIncrement();
    }
}
