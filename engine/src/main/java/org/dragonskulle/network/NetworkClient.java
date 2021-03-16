/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Hex;
import org.dragonskulle.core.GameObject;
import org.dragonskulle.core.Reference;
import org.dragonskulle.core.Scene;
import org.dragonskulle.exceptions.DecodingException;
import org.dragonskulle.network.components.ClientNetworkManager;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.renderer.Font;
import org.dragonskulle.ui.TransformUI;
import org.dragonskulle.ui.UIText;
import org.joml.Vector3f;

/**
 * @author Oscar L
 *     <p>This is the client usage, you will create an instance, by providing the correct server to
 *     connect to. ClientListener is the handler for commands that the client receives. {@link
 *     org.dragonskulle.network.ClientListener}**
 */
public class NetworkClient {
    private static final Logger mLogger = Logger.getLogger(NetworkClient.class.getName());

    /** The Socket connection to the server. */
    private Socket mSocket;
    /** The byte output stream. */
    private DataOutputStream mDOut;
    /** The byte input stream. */
    private BufferedInputStream mBIn;
    /** The Game Instance. */
    private ClientGameInstance mGame;

    /** The Client listener to notify of important events. */
    private ClientListener mClientListener;
    /** True if the socket is open. */
    private boolean mOpen = true;

    /** Stores all requests from the server once scheduled. */
    private final ListenableQueue<byte[]> mRequests = new ListenableQueue<>(new LinkedList<>());
    /** The Runnable for @{mClientThread}. */
    private ClientRunner mClientRunner;
    /**
     * The thread that watches @link{dIn} for messages and adds them to the message
     * queue @link{mRequests}.
     */
    private Thread mClientThread;
    /** True if requests are to be automatically processed every time period. */
    private boolean mAutoProcessMessages;
    /**
     * Schedules the processing of received messages @link {mRequests} runs dependant
     * on @link{mAutoProcessMessages}.
     */
    private final Timer mProcessScheduler = new Timer();

    /**
     * Instantiates a new Network client.
     *
     * @param ip the ip
     * @param port the port
     * @param listener the listener
     * @param autoProcessMessages true if should auto process messages
     */
    public NetworkClient(
            String ip, int port, ClientListener listener, boolean autoProcessMessages) {
        this.mAutoProcessMessages = autoProcessMessages;
        mClientListener = listener;
        try {
            mSocket = new Socket(ip, port);
            mBIn = new BufferedInputStream(mSocket.getInputStream());
            mDOut = new DataOutputStream(mSocket.getOutputStream());
            this.mGame = new ClientGameInstance(this::sendBytes);

            mClientRunner = new ClientRunner();
            mClientThread = new Thread(mClientRunner);
            mClientThread.setName("Client Connection");
            mClientThread.setDaemon(true);
            mClientThread.start();
            listener.connectedToServer();
        } catch (UnknownHostException exception) {
            mOpen = false;
            listener.unknownHost();
        } catch (IOException exception) {
            mOpen = false;
            listener.couldNotConnect();
        } catch (Exception exception) {
            mOpen = false;
            exception.printStackTrace();
        }
    }

    /**
     * Instantiates a new Network client.
     *
     * @param ip the ip
     * @param port the port
     * @param listener the listener
     * @param autoProcessMessages true if should auto process messages
     * @param scene the game scene to be linked
     */
    public NetworkClient(
            String ip,
            int port,
            ClientListener listener,
            boolean autoProcessMessages,
            Scene scene) {
        this.mAutoProcessMessages = autoProcessMessages;
        mClientListener = listener;
        try {
            mSocket = new Socket(ip, port);
            mBIn = new BufferedInputStream(mSocket.getInputStream());
            mDOut = new DataOutputStream(mSocket.getOutputStream());
            this.mGame = new ClientGameInstance(this::sendBytes, scene);

            mClientRunner = new ClientRunner();
            mClientThread = new Thread(mClientRunner);
            mClientThread.setName("Client Connection");
            mClientThread.setDaemon(true);
            mClientThread.start();
            listener.connectedToServer();
        } catch (UnknownHostException exception) {
            mOpen = false;
            listener.unknownHost();
        } catch (IOException exception) {
            mOpen = false;
            listener.couldNotConnect();
        } catch (Exception exception) {
            mOpen = false;
            exception.printStackTrace();
        }
    }

    /**
     * Starts a client game in the game scene, this is started from the menu.
     *
     * @param mainScene the main scene
     * @param ip the ip of the server
     * @param port the port of the server
     */
    public static void startClientGame(Scene mainScene, String ip, int port) {
        GameObject mLoadingScreen =
                new GameObject(
                        "loading_screen",
                        new TransformUI(false),
                        (self) -> {
                            self.addComponent(
                                    new UIText(
                                            new Vector3f(1f, 1f, 0.05f),
                                            Font.getFontResource("Rise of Kingdom.ttf"),
                                            "Loading"));
                        });
        mainScene.addRootObject(mLoadingScreen);
        LogManager.getLogManager().reset();
        final AtomicInteger mNetworkObjectCounter = new AtomicInteger(0);

        ClientListener clientListener = new ClientEars();
        NetworkClient clientInstance =
                new NetworkClient(ip, port, clientListener, false, mainScene);
        GameObject networkManagerGO =
                new GameObject(
                        "client_network_manager",
                        (go) ->
                                go.addComponent(
                                        new ClientNetworkManager(
                                                clientInstance::processRequests,
                                                clientInstance::sendBytes)));

        mLoadingScreen.destroy();
        mainScene.addRootObject(networkManagerGO);
        System.out.println("fully loaded");
    }

    /**
     * Execute bytes after parsing. This is the client version.
     *
     * @param messageType the message type
     * @param payload the payload
     * @return the byteCode of the message processed.
     */
    public byte executeBytes(byte messageType, byte[] payload) {
        mLogger.info("EXEB - " + messageType);
        switch (messageType) {
            case NetworkConfig.Codes.MESSAGE_UPDATE_OBJECT:
                mLogger.fine("Should update requested network object");
                updateNetworkObject(payload);
                break;
            case NetworkConfig.Codes.MESSAGE_SPAWN_OBJECT:
                mLogger.fine("Spawn a networked object");
                spawnNetworkObject(payload);
                break;
            default:
                mLogger.info(
                        "unsure of what to do with message as unknown type byte " + messageType);
                break;
        }
        return messageType;
    }

    /**
     * Update networkable from bytes, this is authored by the server.
     *
     * @param payload the payload
     */
    private void updateNetworkObject(byte[] payload) {
        this.mGame.updateNetworkObject(payload);
    }

    /**
     * Spawn a network object from bytes, this is authored by the server.
     *
     * @param payload payload containing the object info
     */
    private void spawnNetworkObject(byte[] payload) {
        this.mGame.spawnNetworkObject(payload);
    }

    /** Dispose. */
    public void dispose() {
        try {
            if (mOpen) {
                this.sendBytes(new byte[NetworkConfig.MAX_TRANSMISSION_SIZE]);
                mOpen = false;
                closeAllConnections();
                mClientListener.disconnected();
            }
            mSocket = null;
            mDOut = null;
            mClientListener = null;

            mClientRunner.cancel();
            mClientThread.join();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Send a string message to the server.
     *
     * @param msg the msg
     */
    @Deprecated
    public void send(String msg) {
        this.sendBytes(msg.getBytes());
    }

    /**
     * Sends bytes to the server.
     *
     * @param bytes the bytes
     */
    public void sendBytes(byte[] bytes) {
        if (mOpen) {
            try {
                mLogger.fine("sending bytes");
                mDOut.write(bytes);
            } catch (IOException e) {
                mLogger.fine("Failed to send bytes");
            }
        }
    }

    /**
     * Is connected boolean.
     *
     * @return the boolean
     */
    public boolean isConnected() {
        return mOpen;
    }

    /**
     * Gets networkable objects from the game.
     *
     * @return the networkable object references hashed by id
     */
    public HashMap<Integer, Reference<NetworkObject>> getNetworkableObjects() {
        return this.mGame.getNetworkObjects();
    }

    /**
     * Sets the client to process messages automatically or not.
     *
     * @param toggle the toggle
     * @return true once executed, for testing and can be ignored
     */
    public boolean setProcessMessagesAutomatically(boolean toggle) {
        mLogger.fine("Processing Messages Automatically :" + toggle);
        this.mAutoProcessMessages = toggle;
        if (toggle) {
            mProcessScheduler.purge();
            mProcessScheduler.schedule(
                    new ProcessRequestScheduled(),
                    0,
                    30); // bit faster than server game tick rate, // TODO: 11/03/2021 refactor into
            // game object call
        } else {
            mProcessScheduler.purge();
        }
        return true;
    }

    /**
     * Gets the game instance.
     *
     * @return the game
     */
    public ClientGameInstance getGame() {
        return this.mGame;
    }

    /**
     * Sets the linked scene.
     *
     * @param scene the scene
     */
    public void linkToScene(Scene scene) {
        this.mGame.linkToScene(scene);
    }

    /**
     * This is the thread which is created once the connection is achieved. It is used to handle
     * messages received from the server. It also handles the server disconnection.
     */
    private class ClientRunner implements Runnable {

        @Override
        public void run() {
            byte[] bArray;
            byte[] terminateBytes =
                    new byte[NetworkConfig.TERMINATE_BYTES_LENGTH]; // max flatbuffer size
            if (mAutoProcessMessages) {
                setProcessMessagesAutomatically(true);
            }
            while (mOpen && !Thread.currentThread().isInterrupted()) {
                try {
                    bArray = NetworkMessage.readMessageFromStream(mBIn);
                    if (bArray.length != 0) {
                        if (Arrays.equals(bArray, terminateBytes)) {
                            mClientListener.disconnected();
                            dispose();
                            break;
                        } else {
                            queueRequest(bArray);
                        }
                    }

                } catch (IOException ignore) { // if fails to read from in stream
                    if (mClientListener != null) {
                        mClientListener.error("failed to read from input stream");
                    }
                    if (isConnected()) {
                        dispose();
                    }
                    break;
                }
            }
        }

        /** Cancel. */
        public void cancel() {
            setProcessMessagesAutomatically(false);
        }
    }

    /** The Auto Processing requests TimerTask. */
    private class ProcessRequestScheduled extends TimerTask {
        public void run() {
            processRequests();
        }
    }

    /**
     * Queue a new request for the client to process.
     *
     * @param bArray the bytes
     */
    private void queueRequest(byte[] bArray) {
        mLogger.fine("queuing request :: " + Hex.encodeHexString(bArray));
        this.mRequests.addIfUnique(bArray);
    }

    /** Processes all requests. */
    public void processRequests() {
        mLogger.info("processing all " + this.mRequests.size() + " requests");
        while (!this.mRequests.isEmpty()) {
            byte[] requestBytes = this.mRequests.poll();
            if (requestBytes != null) {
                processBytes(requestBytes);
            }
        }
    }

    /**
     * Checks if the client has requests left to process.
     *
     * @return true if there are requests, false otherwise.
     */
    public boolean hasRequests() {
        return !this.mRequests.isEmpty();
    }

    /** Processes a single request. */
    public void processSingleRequest() {
        if (!this.mRequests.isEmpty()) {
            byte[] requestBytes = this.mRequests.poll();
            if (requestBytes != null) {
                mLogger.fine("Processing message with type: " + processBytes(requestBytes));
            }
        }
    }

    /** Clears the pending requests. */
    public void clearPendingRequests() {
        this.mRequests.clear();
    }

    /**
     * Processes bytes from a message.
     *
     * @param bytes the bytes
     */
    private byte processBytes(byte[] bytes) {
        mClientListener.receivedBytes(bytes);
        //        mLogger.fine("parsing bytes :: " + Hex.encodeHexString(bytes));
        try {
            return parseBytes(bytes);
        } catch (DecodingException e) {
            mLogger.fine(e.getMessage());
            mLogger.fine(new String(bytes, StandardCharsets.UTF_8));
            return (byte) -1;
        }
    }

    /**
     * Parses bytes from a message.
     *
     * @param bytes the bytes
     * @throws DecodingException the decoding exception
     */
    private byte parseBytes(byte[] bytes) throws DecodingException {
        try {
            return NetworkMessage.parse(bytes, this);
        } catch (Exception e) {
            mLogger.fine("error parsing bytes");
            e.printStackTrace();
            throw new DecodingException("Message is not of valid type");
        }
    }

    /** Closes all connections. */
    private void closeAllConnections() {
        mOpen = false;

        try {
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        try {
            if (mDOut != null) {
                mDOut.close();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    /** @return True if has a map, false otherwise. */
    public boolean hasMap() {
        return this.mGame.hasSpawnedMap();
    }

    /** @return True if has a capital, false otherwise. */
    public boolean hasCapital() {
        return this.mGame.hasSpawnedCapital();
    }
}
