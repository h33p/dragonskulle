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
import java.util.logging.Logger;

import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.NetworkableComponent;

/**
 * The type Server.
 *
 * @author Oscar L
 * <p>This is the main Server Class, it handles setup and stores all client connections. It can
 * broadcast messages to every client and receive from individual clients.
 */
public class Server {
    private static final Logger mLogger = Logger.getLogger(Server.class.getName());

    /**
     * If the client will automatically process any recieved messages.
     */
    private boolean mAutoProcessMessages = false;
    /**
     * The Port.
     */
    private int mPort;
    /**
     * The Server listener.
     */
    private ServerListener mServerListener;
    /**
     * The socket connections to all clients.
     */
    private final SocketStore mSockets = new SocketStore();
    /**
     * The Server thread.
     */
    private Thread mServerThread;
    /**
     * The Server runner.
     */
    private ServerRunner mServerRunner;
    /**
     * The game instance for the server.
     */
    private ServerGameInstance mGame;
    /**
     * The Network objects - this can be moved to game instance but no point until game has been
     * merged in.
     */
    public final ArrayList<NetworkObject> networkObjects = new ArrayList<>();

    public boolean linkedToScene = false;

    /**
     * The M requests.
     */
    private final ListenableQueue<Request> mRequests = new ListenableQueue<>(new LinkedList<>());
    /**
     * The M network object counter.
     */
    private final AtomicInteger mNetworkObjectCounter;

    private final Timer mFixedUpdate = new Timer();

    /**
     * Instantiates a new Server.
     *
     * @param port     the port
     * @param listener the listener
     */
    public Server(int port, ServerListener listener, AtomicInteger mNetworkObjectCounter) {
        mLogger.info("[S] Setting up server");
        this.mNetworkObjectCounter = mNetworkObjectCounter;
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
            mLogger.info("[S] Starting server");
            mServerThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Instantiates a new Server in debug mode
     *
     * @param port              the port
     * @param listener          the listener
     * @param autoProcessUpdate sets debug mode
     */
    public Server(
            int port,
            ServerListener listener,
            boolean autoProcessUpdate,
            AtomicInteger mNetworkObjectCounter) {
        mLogger.info("[S] Setting up server in debug mode");
        this.mNetworkObjectCounter = mNetworkObjectCounter;
        this.mAutoProcessMessages = autoProcessUpdate;
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
            mLogger.info("[S] Starting server");
            mServerThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Execute bytes on the server.
     *
     * @param messageType       the message type
     * @param payload           the payload
     * @param sendBytesToClient the socket of the requesting client, to be called if a communication
     *                          directly to the client is needed
     */
    public void executeBytes(
            byte messageType, byte[] payload, SendBytesToClientCurry sendBytesToClient) {
        byte[] message;
        mLogger.warning("EXECB - " + messageType);
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
                mLogger.info("Should implement spawn and create building ____");
                message = NetworkMessage.build((byte) 20, "TOSPAWN".getBytes());
                //                sendBytesToClient.send(message);
                break;
        }
    }

    public void startFixedUpdate() {
        int begin = 0;
        int timeInterval = 200;
        FixedUpdate fixedUpdate = this::fixedBroadcastUpdate;
        mFixedUpdate.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        fixedUpdate.call();
                    }
                },
                begin,
                timeInterval);
    }

    public void cancelFixedUpdate() {
        this.mFixedUpdate.cancel();
    }

    /**
     * Client requested respawn byte [ ].
     *
     * @param payload the payload
     * @return the byte [ ]
     */
    private byte[] clientRequestedRespawn(byte[] payload) {
        int componentRequestedId = NetworkMessage.convertByteArrayToInt(payload);
        mLogger.info("The component id to respawn is " + componentRequestedId);
        NetworkableComponent component = this.findComponent(componentRequestedId).get();
        return NetworkMessage.build((byte) 22, component.serializeFully());
    }

    /**
     * Dispose.
     */
    public void dispose() {
        try {
            cancelFixedUpdate();
            this.mServerRunner.cancel();
            this.mServerThread.join();
            this.mSockets.close();
            if (mServerListener != null) {
                this.mServerListener.serverClosed();
                this.mServerListener = null;
            }
        } catch (InterruptedException e) {
            mLogger.info("Error disposing");
            mLogger.info(e.toString());
        }
    }

    /**
     * Create game.
     */
    public void createGame() {
        this.mGame = new ServerGameInstance();
    }

    /**
     * Find networkable component by id.
     *
     * @param componentId the component id
     * @return the networkable component
     */
    public Reference<NetworkableComponent> findComponent(int componentId) {
        Reference<NetworkableComponent> found = null;
        for (NetworkObject e : this.networkObjects) {
            found = e.findComponent(componentId);
            if (found != null) {
                break;
            }
        }
        return found;
    }

    public void spawnObject(NetworkObject networkable) {
        mLogger.warning("spawned object on server");
        networkable.getNetworkableChildren().forEach(e -> e.get().connectSyncVars());
        spawnNetworkObjectOnServer(networkable);
    }

    public void linkToScene(Scene mainScene) {
        this.linkedToScene = true;
        this.mGame.setScene(mainScene);
    }

    /**
     * ServerRunner is the thread which constantly checks for new client requests, if a client has
     * requested a socket, it will provide it a thread to communicate on and accept the socket.
     * SO_TIMEOUT is set so that sockets.acceptClient won't block the joining of the thread
     * indefinitely.
     */
    private class ServerRunner implements Runnable {
        /**
         * The Open.
         */
        volatile boolean mOpen = true;

        /**
         * The M process timer.
         */
        private final Timer mProcessTimer = new Timer();

        @Override
        public void run() {
            while (mOpen && !Thread.currentThread().isInterrupted()) {
                if (mGame.isSetup()) {
                    if (!mAutoProcessMessages) {
                        mProcessTimer.schedule(new FixedBroadCastUpdateSchedule(), 0, 1500);
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

        /**
         * Cancel.
         */
        public void cancel() {
            this.mOpen = false;
            this.mProcessTimer.cancel();
        }
    }

    /**
     * The type Fixed broad cast update schedule.
     */
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
            return () -> {
            };
        }
        return () -> {
            try {
                mLogger.info("Spawning client thread");
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
                    NetworkObject networkObject = new NetworkObject(this.allocateId(), true);
                    if (linkedToScene) {
                        networkObject.linkToScene();
                    }
                    networkObject.spawnMap(
                            this.mGame.cloneMap(),
                            (message) -> this.mSockets.sendBytesToClient(client, message));
                    int capitalId =
                            networkObject.serverSpawnCapital(
                                    networkObject.getId(), this.mSockets::broadcast);
                    spawnNetworkObjectOnServer(networkObject);
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

    private void spawnNetworkObjectOnServer(NetworkObject networkObject) {
        if (linkedToScene) {
            this.mGame.spawnNetworkObjectOnScene(networkObject);
        }
        this.networkObjects.add(networkObject);
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

    /**
     * Process requests.
     */
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

    public int spawnComponent(NetworkableComponent component, int networkObjectToSpawnOn) {
        NetworkObject networkObject = this.getNetworkObject(networkObjectToSpawnOn);
        if (networkObject != null) {
            int componentId =
                    networkObject.serverSpawnComponent(component, this.mSockets::broadcast);
            spawnNetworkObjectOnServer(networkObject);
            return componentId;
        }
        return -1;
    }

    private NetworkObject getNetworkObject(int networkObjectToSpawnOn) {
        return this.networkObjects.stream()
                .filter(e -> e.getId() == networkObjectToSpawnOn)
                .findFirst()
                .orElse(null);
    }

    /**
     * Has requests boolean.
     *
     * @return the boolean
     */
    public boolean hasRequests() {
        return !this.mRequests.isEmpty();
    }

    /**
     * Process single request.
     */
    public void processSingleRequest() {
        if (!this.mRequests.isEmpty()) {
            Request request = this.mRequests.poll();
            if (request != null) {
                processBytes(request.client, request.bytes);
            }
        }
    }

    /**
     * Clear pending requests.
     */
    public void clearPendingRequests() {
        this.mRequests.clear();
    }

    /**
     * Gets networkable child.
     *
     * @param networkObject the network object
     * @param id            the id
     * @return the networkable child
     */
    private NetworkableComponent getNetworkableChild(NetworkObject networkObject, int id) {
        final NetworkObject serverNetworkObject =
                this.networkObjects.get(this.networkObjects.indexOf(networkObject));
        return serverNetworkObject.get(id).get();
    }

    /**
     * Process bytes.
     *
     * @param client the client
     * @param bytes  the bytes
     */
    private void processBytes(ClientInstance client, byte[] bytes) {

        mServerListener.receivedBytes(client, bytes);
        try {
            parseBytes(client, bytes);
        } catch (DecodingException e) {
            mLogger.info(e.getMessage());
            mLogger.info(new String(bytes, StandardCharsets.UTF_8));
        }
    }

    /**
     * Parse bytes.
     *
     * @param client the client
     * @param bytes  the bytes
     * @throws DecodingException Thrown if there was any issue with the bytes
     */
    private void parseBytes(ClientInstance client, byte[] bytes) throws DecodingException {
        //        mLogger.info("bytes parsing");
        try {
            parse(bytes, (parsedBytes) -> this.mSockets.sendBytesToClient(client, parsedBytes));
        } catch (Exception e) {
            mLogger.info("Error in parseBytes");
            e.printStackTrace();
            throw new DecodingException("Message is not of valid type");
        }
    }

    /**
     * Parses a network message from bytes and executes the correct functions. This is for server
     * use.
     *
     * @param buff              the buff
     * @param sendBytesToClient the send bytes to client
     */
    public void parse(byte[] buff, SendBytesToClientCurry sendBytesToClient) {
        if (buff.length == 0 || Arrays.equals(buff, new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0})) {
            return;
        }
        int i = 0;
        boolean validStart = NetworkMessage.verifyMessageStart(buff);
        i += 5;
        if (validStart) {
            //            mLogger.info("Valid Message Start\n");
            byte messageType = NetworkMessage.getMessageType(buff);
            i += 1;
            int payloadSize = NetworkMessage.getPayloadSize(buff);
            i += 4;
            byte[] payload = NetworkMessage.getPayload(buff, messageType, i, payloadSize);
            i += payloadSize;
            boolean consumedMessage = NetworkMessage.verifyMessageEnd(i, buff);
            if (consumedMessage) {
                if (messageType == (byte) 0) {
                    mLogger.info("\nValid Message");
                    mLogger.info("Type : " + messageType);
                    mLogger.info("Payload : " + Arrays.toString(payload));
                } else {
                    executeBytes(messageType, payload, sendBytesToClient);
                }
            }
        } else {
            mLogger.info("invalid message start");
        }
    }

    /**
     * The interface Send bytes to client curry.
     */
    public interface SendBytesToClientCurry {
        /**
         * Send.
         *
         * @param bytes the bytes
         */
        void send(byte[] bytes);
    }

    /**
     * The interface Fixed update simulation.
     */
    public interface FixedUpdate {
        /**
         * Call.
         */
        void call();
    }

    /**
     * Fixed broadcast update.
     */
    public void fixedBroadcastUpdate() {
        mLogger.warning(networkObjects.toString());

        processRequests();
        for (NetworkObject networkObject : this.networkObjects) {
            networkObject.broadcastUpdate(this.mSockets::broadcast);
        }
    }

    /**
     * The type Request.
     */
    private class Request {
        /**
         * The Client.
         */
        public final ClientInstance client;
        /**
         * The Bytes.
         */
        public final byte[] bytes;

        /**
         * Instantiates a new Request.
         *
         * @param client the client
         * @param bytes  the bytes
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
