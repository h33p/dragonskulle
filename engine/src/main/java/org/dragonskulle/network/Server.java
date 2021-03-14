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
import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.exceptions.DecodingException;
import org.dragonskulle.network.components.NetworkObject;

/**
 * The type Server.
 *
 * @author Oscar L
 *     <p>This is the main Server Class, it handles setup and stores all client connections. It can
 *     broadcast messages to every client and receive from individual clients.
 */
@Accessors(prefix = "m")
public class Server {
    private static final Logger mLogger = Logger.getLogger(Server.class.getName());

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
    @Getter
    public final HashMap<Integer, Reference<NetworkObject>> mNetworkObjects = new HashMap<>();

    /** true if linked to a game scene. */
    public boolean linkedToScene = false;

    /** The scheduled requests to be processed. */
    private final ListenableQueue<Request> mRequests = new ListenableQueue<>(new LinkedList<>());
    /** The Counter used to assign objects a unique id. */
    private final AtomicInteger mNetworkObjectCounter;

    /** Used to run @link{mFixedUpdate} when not linked to a game scene, for testing. */
    private final Timer mFixedUpdate = new Timer();

    /**
     * Instantiates a new Server.
     *
     * @param port the port
     * @param listener the listener
     * @param mNetworkObjectCounter the networkCounter from its parent, this is so id's are globally
     *     in sync.
     */
    public Server(int port, ServerListener listener, AtomicInteger mNetworkObjectCounter) {
        mLogger.fine("[S] Setting up server");
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
            mLogger.fine("[S] Starting server");
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
     * @param autoProcessUpdate sets debug mode
     * @param mNetworkObjectCounter the networkCounter from its parent, this is so id's are globally
     *     in sync
     */
    public Server(
            int port,
            ServerListener listener,
            boolean autoProcessUpdate,
            AtomicInteger mNetworkObjectCounter) {
        mLogger.fine("[S] Setting up server in debug mode");
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
            mLogger.fine("[S] Starting server");
            mServerThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes bytes on the server.
     *
     * @param messageType the message type
     * @param payload the payload
     * @param sendBytesToClient the socket of the requesting client, to be called if a communication
     *     directly to the client is needed
     */
    public void executeBytes(
            byte messageType, byte[] payload, SendBytesToClientCurry sendBytesToClient) {
        // byte[] message;
        mLogger.info("EXECB - " + messageType);
        switch (messageType) {
            case NetworkConfig.Codes.MESSAGE_CLIENT_REQUEST:
                handleClientRequest(payload);
                break;
                // TODO: here implement a generic way to pass client->server commands
                /*case (byte) 22:
                message = NetworkMessage.build((byte) 20, "TOSPAWN".getBytes());
                //                sendBytesToClient.send(message);
                break;*/
            default:
                mLogger.info(
                        "The server received a request from a client to do something "
                                + messageType);
                // message = NetworkMessage.build((byte) 20, "TOSPAWN".getBytes());
                //                sendBytesToClient.send(message);
                break;
        }
    }

    private void handleClientRequest(byte[] payload) {
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(payload)) {
            try (DataInputStream stream = new DataInputStream(bytes)) {
                int objectID = stream.readInt();

                Reference<NetworkObject> networkObject = getNetworkObject(objectID);

                // TODO: Authenticate here whether this particular client is authorized to invoke
                // requests on this object

                if (networkObject == null) {
                    mLogger.info("Client sent request with invalid object ID! " + objectID);
                    return;
                }

                NetworkObject obj = networkObject.get();

                // Normal to happen if object gets destroyed after client sent their request
                if (obj == null) {
                    mLogger.fine("Client made a request on already destroyed object! " + objectID);
                }

                int requestID = stream.readInt();

                if (!obj.handleClientRequest(requestID, stream))
                    mLogger.warning("Client passed invalid request! " + requestID);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Starts fixed update task. */
    public void startFixedUpdateDetachedFromGame() {
        int begin = 0;
        int timeInterval = 200;
        FixedUpdate fixedUpdate = this::fixedBroadcastUpdate;
        mFixedUpdate.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        if (mAutoProcessMessages) {
                            fixedUpdate.call();
                        }
                    }
                },
                begin,
                timeInterval);
    }

    /** Cancels the scheduled fixed update task. */
    public void cancelFixedUpdate() {
        this.mFixedUpdate.cancel();
    }

    /** Dispose. */
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
            mLogger.fine("Error disposing");
            mLogger.fine(e.toString());
        }
    }

    /** Creates game. */
    public void createGame() {
        this.mGame = new ServerGameInstance();
    }

    /**
     * Links the server to scene, this doesn't have to be used if testing.
     *
     * @param scene the game scene
     */
    public void linkToScene(Scene scene) {
        this.linkedToScene = true;
        this.mGame.setScene(scene);
    }

    public void viewPendingRequests() {
        System.out.println(this.mRequests.toString());
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
                if (mGame.isSetup()) {
                    //                    if (!mAutoProcessMessages) {
                    //                        mProcessTimer.schedule(new
                    // FixedBroadCastUpdateSchedule(), 0, 400);
                    //                    }
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
        }
    }

    /** The interface for a fixed update broadcast event. */
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
                mLogger.fine("Spawning client thread");
                boolean connected;
                int hasBytes = 0;
                byte[] bArray; // max flatbuffer size
                byte[] terminateBytes =
                        new byte[NetworkConfig.TERMINATE_BYTES_LENGTH]; // max flatbuffer size
                this.mSockets.addClient(sock);

                BufferedInputStream bIn = new BufferedInputStream(sock.getInputStream());
                PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                // create client as object
                ClientInstance client = new ClientInstance(sock.getInetAddress(), sock.getPort());
                mServerListener.clientConnected(client, out);
                connected = sock.isConnected();

                if (connected) {
                    // Spawn network object for the cube thingy and capital
                    spawnNetworkObject(client, Templates.find("cube"));
                    spawnNetworkObject(client, Templates.find("capital"));
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
     * Spawns a network object on server, if linked to a game it will also spawn it on the game.
     *
     * @param templateId ID of spawnable template
     */
    private Reference<NetworkObject> spawnNetworkObject(ClientInstance client, int templateId) {
        int netId = this.allocateId();

        NetworkObject networkObject = new NetworkObject(netId, true);
        GameObject object = Templates.instantiate(templateId);
        object.addComponent(networkObject);
        Reference<NetworkObject> ref = networkObject.getReference(NetworkObject.class);

        if (linkedToScene) {
            this.mGame.spawnNetworkObjectOnScene(networkObject);
        } else {
            networkObject.onAwake();
        }

        this.mNetworkObjects.put(netId, ref);

        byte[] spawnMessage =
                NetworkMessage.build(
                        NetworkConfig.Codes.MESSAGE_SPAWN_OBJECT,
                        NetworkMessage.convertIntsToByteArray(netId, templateId));
        this.mSockets.sendBytesToClient(client, spawnMessage);

        return ref;
    }

    /**
     * Queues a request to be processed.
     *
     * @param client the client
     * @param bArray the bytes to be processed later
     */
    private void queueRequest(ClientInstance client, byte[] bArray) {
        this.mRequests.add(new Request(client, bArray));
    }

    /** Process all requests. */
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
     * Gets a network object.
     *
     * @param networkObjectId the id of the object
     * @return the network object found, null if not found
     */
    private Reference<NetworkObject> getNetworkObject(int networkObjectId) {
        return this.mNetworkObjects.get(networkObjectId);
    }

    /** @return true if the server has unprocess requests */
    public boolean hasRequests() {
        return !this.mRequests.isEmpty();
    }

    /** Processes a single request. */
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
     * Processes bytes.
     *
     * @param client the client
     * @param bytes the bytes
     */
    private void processBytes(ClientInstance client, byte[] bytes) {
        mServerListener.receivedBytes(client, bytes);
        try {
            parseBytes(client, bytes);
        } catch (DecodingException e) {
            mLogger.fine(e.getMessage());
            mLogger.fine(new String(bytes, StandardCharsets.UTF_8));
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
        mLogger.warning("bytes parsing");
        try {
            parse(bytes, (parsedBytes) -> this.mSockets.sendBytesToClient(client, parsedBytes));
        } catch (Exception e) {
            mLogger.fine("Error in parseBytes");
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
            //            mLogger.fine("Valid Message Start\n");
            byte messageType = NetworkMessage.getMessageType(buff);
            i += 1;
            int payloadSize = NetworkMessage.getPayloadSize(buff);
            i += 4;
            byte[] payload = NetworkMessage.getPayload(buff, messageType, i, payloadSize);
            i += payloadSize;
            boolean consumedMessage = NetworkMessage.verifyMessageEnd(i, buff);
            if (consumedMessage) {
                if (messageType == (byte) 0) {
                    mLogger.fine("\nValid Message");
                    mLogger.fine("Type : " + messageType);
                    mLogger.fine("Payload : " + Arrays.toString(payload));
                } else {
                    executeBytes(messageType, payload, sendBytesToClient);
                }
            }
        } else {
            mLogger.fine("invalid message start");
        }
    }

    /** The interface Send bytes to client curry. */
    interface SendBytesToClientCurry {
        /**
         * Send.
         *
         * @param bytes the bytes
         */
        void send(byte[] bytes);
    }

    /** Fixed broadcast update. */
    public void fixedBroadcastUpdate() {
        mLogger.info(mNetworkObjects.toString());

        processRequests();
        for (Reference<NetworkObject> networkObject : this.mNetworkObjects.values()) {
            networkObject.get().broadcastUpdate(this.mSockets::broadcast);
        }
    }

    /** The type Request. */
    private static class Request {
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
     * Allocates an id for an object.
     *
     * @return the allocated id.
     */
    private int allocateId() {
        return mNetworkObjectCounter.getAndIncrement();
    }
}
