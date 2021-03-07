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
 * The type Server.
 *
 * @author Oscar L
 *     <p>This is the main Server Class, it handles setup and stores all client connections. It can
 *     broadcast messages to every client and receive from individual clients.
 */
public class Server {

    /** If the client will automatically process any recieved messages. */
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

    /** The M requests. */
    private final ListenableQueue<Request> mRequests = new ListenableQueue<>(new LinkedList<>());
    /** The M network object counter. */
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
    public void executeBytes(
            byte messageType, byte[] payload, SendBytesToClientCurry sendBytesToClient) {
        byte[] message;
        switch (messageType) {
            case (byte) 22:
                message = NetworkMessage.build((byte) 20, "TOSPAWN".getBytes());
                //                sendBytesToClient.send(message);
                break;
            case (byte) 50:
                message = clientRequestedRespawn(payload);
                sendBytesToClient.send(message);
                break;
            default:
                System.out.println("Should implement spawn and create building ____");
                message = NetworkMessage.build((byte) 20, "TOSPAWN".getBytes());
                //                sendBytesToClient.send(message);
                break;
        }
    }

    /**
     * Client requested respawn byte [ ].
     *
     * @param payload the payload
     * @return the byte [ ]
     */
    private byte[] clientRequestedRespawn(byte[] payload) {
        int componentRequestedId = NetworkMessage.convertByteArrayToInt(payload);
        System.out.println("The component id to respawn is " + componentRequestedId);
        NetworkableComponent component = this.findComponent(componentRequestedId);
        return NetworkMessage.build((byte) 22, component.serializeFully());
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

        /** The M process timer. */
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

    /** The type Fixed broad cast update schedule. */
    private class FixedBroadCastUpdateSchedule extends TimerTask {
        public void run() {
            processRequests();
        }
    }

    /**
     * THe Client Runner is the thread given to each client to handle its own socket. Commands are
     * read from the input stream. It will pass all commands to the correct handler function. {@link
     * org.dragonskulle.network.ServerListener}***
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
                                    if (counter >= 100) {
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

    /**
     * Queue request.
     *
     * @param client the client
     * @param bArray the b array
     */
    private void queueRequest(ClientInstance client, byte[] bArray) {
        this.mRequests.add(new Request(client, bArray));
    }

    /** Process requests. */
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

    /**
     * Has requests boolean.
     *
     * @return the boolean
     */
    public boolean hasRequests() {
        return !this.mRequests.isEmpty();
    }

    /** Process single request. */
    public void processSingleRequest() {
        if (!this.mRequests.isEmpty()) {
            Request request = this.mRequests.poll();
            if (request != null) {
                processBytes(request.client, request.bytes);
            }
        }
    }

    /** Clear pending requests. */
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
        //        System.out.println("bytes parsing");
        try {
            parse(bytes, (parsedBytes) -> this.mSockets.sendBytesToClient(client, parsedBytes));
        } catch (Exception e) {
            System.out.println("Error in parseBytes");
            e.printStackTrace();
            throw new DecodingException("Message is not of valid type");
        }
    }

    /**
     * Parses a network message from bytes and executes the correct functions. This is for server
     * use.
     *
     * @param buff the buff
     * @param sendBytesToClient the send bytes to client
     */
    public void parse(byte[] buff, SendBytesToClientCurry sendBytesToClient) {
        if (buff.length == 0 || Arrays.equals(buff, new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0})) {
            return;
        }
        int i = 0;
        boolean validStart = NetworkMessage.verifyMessageStart(buff);
        i += 5;
        if (validStart) {
            //            System.out.println("Valid Message Start\n");
            byte messageType = NetworkMessage.getMessageType(buff);
            i += 1;
            int payloadSize = NetworkMessage.getPayloadSize(buff);
            i += 4;
            byte[] payload = NetworkMessage.getPayload(buff, messageType, i, payloadSize);
            i += payloadSize;
            boolean consumedMessage = NetworkMessage.verifyMessageEnd(i, buff);
            if (consumedMessage) {
                if (messageType == (byte) 0) {
                    System.out.println("\nValid Message");
                    System.out.println("Type : " + messageType);
                    System.out.println("Payload : " + Arrays.toString(payload));
                } else {
                    executeBytes(messageType, payload, sendBytesToClient);
                }
            }
        } else {
            System.out.println("invalid message start");
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

    /** The type Request. */
    private class Request {
        /** The Client. */
        public final ClientInstance client;
        /** The Bytes. */
        public final byte[] bytes;

        /**
         * Instantiates a new Request.
         *
         * @param client the client
         * @param bytes the bytes
         */
        Request(ClientInstance client, byte[] bytes) {
            this.client = client;
            this.bytes = bytes;
        }
    }

    /**
     * Allocate id int.
     *
     * @return the int
     */
    private int allocateId() {
        return mNetworkObjectCounter.getAndIncrement();
    }
}
