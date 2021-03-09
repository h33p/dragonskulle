/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Hex;
import org.dragonskulle.network.components.Capital;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.NetworkableComponent;

/**
 * @author Oscar L
 *     <p>This is the client usage, you will create an instance, by providing the correct server to
 *     connect to. ClientListener is the handler for commands that the client receives. {@link
 *     org.dragonskulle.network.ClientListener}**
 */
public class NetworkClient {
    private final Logger mLogger = Logger.getLogger(this.getClass().getName());

    /** The constant MAX_TRANSMISSION_SIZE. */
    private static final int MAX_TRANSMISSION_SIZE = 512;
    /** The Socket connection to the server. */
    private Socket mSocket;
    /** The Input stream. Possibly depreciated in favour of byte streams. */
    private BufferedReader mIn;
    /** The Output stream. Possibly depreciated in favour of byte streams. */
    private PrintWriter mOut;
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

    private int mCapitalId;
    private final ListenableQueue<byte[]> mRequests = new ListenableQueue<>(new LinkedList<>());
    private ClientRunner mClientRunner;
    private Thread mClientThread;
    private boolean mAutoProcessMessages = false;
    private final Timer mProcessScheduler = new Timer();

    /**
     * Instantiates a new Network client.
     *
     * @param ip the ip
     * @param port the port
     * @param listener the listener
     * @param autoProcessMessages the auto process messages
     */
    public NetworkClient(
            String ip, int port, ClientListener listener, boolean autoProcessMessages) {
        this.mAutoProcessMessages = autoProcessMessages;
        mClientListener = listener;
        try {
            mSocket = new Socket(ip, port);
            mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            mBIn = new BufferedInputStream(mSocket.getInputStream());
            mOut = new PrintWriter(mSocket.getOutputStream(), true);
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
     * Execute bytes after parsing. This will be different usage depending on server or client.
     *
     * @param messageType the message type
     * @param payload the payload
     * @return the byte
     */
    public byte executeBytes(byte messageType, byte[] payload) {
        switch (messageType) {
            case (byte) 15:
                mLogger.info("Should update requested network object");
                updateNetworkObject(payload);
                break;
            case (byte) 20:
                mLogger.info("Trying to spawn map, need to get the actual map");
                this.mGame.spawnMap(payload);
                mLogger.info("Spawned map");
                break;
            case (byte) 21:
                try {
                    mLogger.info("Trying to spawn capital");
                    Capital capital = deserializeCapitol(payload);
                    this.mCapitalId = this.mGame.spawnCapital(capital.getOwnerId(), capital);
                    if (capital.getId() == mCapitalId) {
                        mLogger.info("Spawned capital");
                    }
                } catch (DecodingException e) {
                    e.printStackTrace();
                }
                break;
            case (byte) 22:
                mLogger.info("Trying to spawn component");
                NetworkableComponent component = NetworkableComponent.createFromBytes(payload);
                assert component != null;
                int componentId = this.mGame.spawnComponent(component.getOwnerId(), component);
                mLogger.info("Spawned Component");
                break;
            default:
                mLogger.info(
                        "unsure of what to do with message as unknown type byte " + messageType);
                break;
        }
        return messageType;
    }

    /**
     * Deserialize the capital bytes.
     *
     * @param payload the payload
     * @return the capital
     * @throws DecodingException Thrown if any errors occur in deserialization.
     */
    private Capital deserializeCapitol(byte[] payload) throws DecodingException {
        return NetworkableComponent.from(Capital.class, payload);
    }

    /**
     * Update networkable from bytes, this is authored by the server.
     *
     * @param payload the payload
     */
    private void updateNetworkObject(byte[] payload) {
        this.mGame.updateNetworkObject(payload);
    }

    /** Dispose. */
    public void dispose() {
        try {
            if (mOpen) {
                this.sendBytes(new byte[MAX_TRANSMISSION_SIZE]);
                mOpen = false;
                closeAllConnections();
                mClientListener.disconnected();
            }
            mSocket = null;
            mIn = null;
            mOut = null;
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
    public void send(String msg) {
        this.sendBytes(msg.getBytes());
    }

    /**
     * Send bytes to the server.
     *
     * @param bytes the bytes
     */
    public void sendBytes(byte[] bytes) {
        if (mOpen) {
            try {
                mLogger.info("sending bytes");
                mDOut.write(bytes);
            } catch (IOException e) {
                mLogger.info("Failed to send bytes");
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

    public ArrayList<NetworkObject> getNetworkableObjects() {
        return this.mGame.getNetworkObjects();
    }

    public boolean setProcessMessagesAutomatically(boolean toggle) {
        mLogger.info("Processing Messages Automatically :" + toggle);
        this.mAutoProcessMessages = toggle;
        if (toggle) {
            mProcessScheduler.schedule(new ProcessRequestScheduled(), 0, 1000);
        } else {
            mProcessScheduler.cancel();
        }
        return true;
    }

    public ClientGameInstance getGame() {
        return this.mGame;
    }

    /**
     * This is the thread which is created once the connection is achieved. It is used to handle
     * messages received from the server. It also handles the server disconnection.
     */
    private class ClientRunner implements Runnable {

        @Override
        public void run() {
            byte[] bArray;
            byte[] terminateBytes = new byte[MAX_TRANSMISSION_SIZE]; // max flatbuffer size
            if (mAutoProcessMessages) {
                mProcessScheduler.schedule(new ProcessRequestScheduled(), 0, 1000);
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

    private class ProcessRequestScheduled extends TimerTask {
        public void run() {
            processRequests();
        }
    }

    private void queueRequest(byte[] bArray) {
        mLogger.info("queuing request :: " + Hex.encodeHexString(bArray));
        this.mRequests.add(bArray);
    }

    public void processRequests() {
        mLogger.warning("processing all " + this.mRequests.size() + " requests");
        while (!this.mRequests.isEmpty()) {
            byte[] requestBytes = this.mRequests.poll();
            if (requestBytes != null) {
                processBytes(requestBytes);
            }
        }
    }

    public boolean hasRequests() {
        return !this.mRequests.isEmpty();
    }

    public void processSingleRequest() {
        if (!this.mRequests.isEmpty()) {
            byte[] requestBytes = this.mRequests.poll();
            if (requestBytes != null) {
                mLogger.info("Processing message with type: " + processBytes(requestBytes));
            }
        }
    }

    public void clearPendingRequests() {
        this.mRequests.clear();
    }

    /**
     * Process bytes.
     *
     * @param bytes the bytes
     */
    private byte processBytes(byte[] bytes) {
        mClientListener.receivedBytes(bytes);
        //        mLogger.info("parsing bytes :: " + Hex.encodeHexString(bytes));
        try {
            return parseBytes(bytes);
        } catch (DecodingException e) {
            mLogger.info(e.getMessage());
            mLogger.info(new String(bytes, StandardCharsets.UTF_8));
            return (byte) -1;
        }
    }

    /**
     * Parse bytes.
     *
     * @param bytes the bytes
     * @throws DecodingException the decoding exception
     */
    private byte parseBytes(byte[] bytes) throws DecodingException {
        try {
            return NetworkMessage.parse(bytes, this);
        } catch (Exception e) {
            mLogger.info("error parsing bytes");
            e.printStackTrace();
            throw new DecodingException("Message is not of valid type");
        }
    }

    /** Close all connections. */
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
            if (mIn != null) {
                mIn.close();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        try {
            if (mOut != null) {
                mOut.close();
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

    public boolean hasMap() {
        return this.mGame.hasSpawnedMap();
    }

    public Boolean hasCapital() {
        return this.mGame.hasSpawnedCapital();
    }

    public int getCapitalId() {
        return this.mCapitalId;
    }

    public NetworkableComponent getNetworkableComponent(int networkableId) {
        return this.mGame.getNetworkedComponent(networkableId);
    }
}
