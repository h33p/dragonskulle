/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.dragonskulle.network.components.Capital;
import org.dragonskulle.network.components.NetworkableComponent;

/**
 * @author Oscar L
 *     <p>This is the client usage, you will create an instance, by providing the correct server to
 *     connect to. ClientListener is the handler for commands that the client receives. {@link
 *     org.dragonskulle.network.ClientListener}**
 */
public class NetworkClient {
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

    /**
     * Instantiates a new Network client.
     *
     * @param ip the ip
     * @param port the port
     * @param listener the listener
     */
    public NetworkClient(String ip, int port, ClientListener listener) {
        mClientListener = listener;
        try {
            mSocket = new Socket(ip, port);
            mIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            mBIn = new BufferedInputStream(mSocket.getInputStream());
            mOut = new PrintWriter(mSocket.getOutputStream(), true);
            mDOut = new DataOutputStream(mSocket.getOutputStream());
            this.mGame = new ClientGameInstance();
            Thread clientThread = new Thread(this.clientRunner());
            clientThread.setName("Client Connection");
            clientThread.setDaemon(true);
            clientThread.start();
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
     */
    public void executeBytes(byte messageType, byte[] payload) {
        switch (messageType) {
            case (byte) 10:
                System.out.println("Should update requested component");
                System.out.println("Current component is");
                String networkableId = NetworkableComponent.getIdFromBytes(payload);
                this.mGame.printNetworkable(networkableId);
                updateNetworkable(payload);
                System.out.println("Component after update");
                this.mGame.printNetworkable(networkableId);
                break;
            case (byte) 20:
                System.out.println("Trying to spawn map, need to get the actual map");
                this.mGame.spawnMap(payload);
                System.out.println("Spawned map");
                break;
            case (byte) 21:
                try {
                    System.out.println("Trying to spawn capital");
                    Capital capital = deserializeCapitol(payload);
                    System.out.println("deserialized capital bytes, now spawning locally");
                    this.mGame.spawnCapital(capital);
                    System.out.println("Spawned capital");
                } catch (DecodingException e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.out.println(
                        "unsure of what to do with message as unknown type byte " + messageType);
                break;
        }
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
    private void updateNetworkable(byte[] payload) {
        System.out.println("Starting to update networkable");
        this.mGame.updateNetworkable(payload);
        System.out.println("updated networkable");
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
                System.out.println("sending bytes");
                mDOut.write(bytes);
            } catch (IOException e) {
                System.out.println("Failed to send bytes");
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
     * This is the thread which is created once the connection is achieved. It is used to handle
     * messages received from the server. It also handles the server disconnection.
     *
     * @return The Client Thread.
     */
    private Runnable clientRunner() {

        return () -> {
            byte[] bArray;
            byte[] terminateBytes = new byte[MAX_TRANSMISSION_SIZE]; // max flatbuffer size
            while (mOpen) {
                try {
                    bArray = NetworkMessage.readMessageFromStream(mBIn);
                    if (bArray.length != 0) {
                        if (Arrays.equals(bArray, terminateBytes)) {
                            mClientListener.disconnected();
                            this.dispose();
                            break;
                        } else {
                            processBytes(bArray);
                        }
                    }

                } catch (IOException ignore) { // if fails to read from in stream
                    if (mClientListener != null) {
                        mClientListener.error("failed to read from input stream");
                    }
                    if (this.isConnected()) {
                        this.dispose();
                    }
                    break;
                }
            }
        };
    }

    /**
     * Process bytes.
     *
     * @param bytes the bytes
     */
    private void processBytes(byte[] bytes) {
        mClientListener.receivedBytes(bytes);
        try {
            parseBytes(bytes);
        } catch (DecodingException e) {
            System.out.println(e.getMessage());
            System.out.println(new String(bytes, StandardCharsets.UTF_8));
        }
    }

    /**
     * Parse bytes.
     *
     * @param bytes the bytes
     * @throws DecodingException the decoding exception
     */
    private void parseBytes(byte[] bytes) throws DecodingException {
        System.out.println("bytes unpacking");
        try {
            NetworkMessage.parse(bytes, this);
        } catch (Exception e) {
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

    public String getCapitalId() {
        return this.mGame.getNetworkedComponents().stream()
                .filter(e -> e.getClass().isAssignableFrom(Capital.class))
                .findFirst()
                .orElseGet(null)
                .getId();
    }

    public NetworkableComponent getNetworkableComponent(String networkableId) {
        return this.mGame.getNetworkedComponents(networkableId);
    }
}
