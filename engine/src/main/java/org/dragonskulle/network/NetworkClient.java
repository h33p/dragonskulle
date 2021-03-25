/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Hex;
import org.dragonskulle.exceptions.DecodingException;

/**
 * @author Oscar L
 *     <p>This is the client usage, you will create an instance, by providing the correct server to
 *     connect to. ClientListener is the handler for commands that the client receives. {@link
 *     org.dragonskulle.network.IClientListener}**
 */
public class NetworkClient {
    private static final Logger mLogger = Logger.getLogger(NetworkClient.class.getName());

    /** The Socket connection to the server. */
    private Socket mSocket;
    /** The byte output stream. */
    private DataOutputStream mDOut;
    /** The byte input stream. */
    private BufferedInputStream mBIn;

    /** The Client listener to notify of important events. */
    private IClientListener mClientListener;
    /** True if the socket is open. */
    private boolean mOpen = true;

    /** Stores all requests from the server once scheduled. */
    private final ConcurrentLinkedQueue<byte[]> mRequests = new ConcurrentLinkedQueue<>();
    /** The Runnable for @{mClientThread}. */
    private ClientRunner mClientRunner;

    /**
     * The thread that watches @link{dIn} for messages and adds them to the message
     * queue @link{mRequests}.
     */
    private Thread mClientThread;

    private AtomicBoolean didDispose = new AtomicBoolean(false);

    /**
     * Instantiates a new Network client.
     *
     * @param ip the ip
     * @param port the port
     * @param listener the listener
     */
    public NetworkClient(String ip, int port, IClientListener listener) {
        mClientListener = listener;
        try {
            mClientRunner = new ClientRunner();
            mClientRunner.mIP = ip;
            mClientRunner.mPort = port;

            mClientThread = new Thread(mClientRunner);
            mClientThread.setName("Client Connection");
            mClientThread.setDaemon(true);
            mClientThread.start();
        } catch (Exception exception) {
            mOpen = false;
            exception.printStackTrace();
        }
    }

    /**
     * Execute bytes after parsing. This is the client version.
     *
     * @param messageType the message type
     * @param payload the payload
     * @return the byteCode of the message processed.
     */
    public byte executeBytes(byte messageType, byte[] payload) {
        mLogger.fine("EXEB - " + messageType);
        switch (messageType) {
            case NetworkConfig.Codes.MESSAGE_UPDATE_OBJECT:
                mLogger.fine("Should update requested network object");
                mClientListener.updateNetworkObject(payload);
                break;
            case NetworkConfig.Codes.MESSAGE_SPAWN_OBJECT:
                mLogger.fine("Spawn a networked object");
                mClientListener.spawnNetworkObject(payload);
                break;
            default:
                mLogger.info(
                        "unsure of what to do with message as unknown type byte " + messageType);
                break;
        }
        return messageType;
    }

    /** Dispose. */
    public void dispose() {
        try {
            if (!didDispose.get()) {
                didDispose.set(true);
                if (mOpen) {
                    this.sendBytes(new byte[NetworkConfig.MAX_TRANSMISSION_SIZE]);
                    mOpen = false;
                    closeAllConnections();
                    if (mClientListener != null) {
                        mClientListener.disconnected();
                    }
                }
                mSocket = null;
                mDOut = null;
                mClientListener = null;
                if (mClientThread != null) {
                    mClientThread.interrupt();
                    mClientThread.join();
                }
            }
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
                if (mDOut != null) {
                    mLogger.fine("sending bytes");
                    mDOut.write(bytes);
                }
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
     * This is the thread which is created once the connection is achieved. It is used to handle
     * messages received from the server. It also handles the server disconnection.
     */
    @SuppressWarnings("")
    private class ClientRunner implements Runnable {
        final AtomicBoolean didTryDispose = new AtomicBoolean(false);

        private String mIP;
        private int mPort;

        @Override
        public void run() {
            try {
                mSocket = new Socket(mIP, mPort);
                mBIn = new BufferedInputStream(mSocket.getInputStream());
                mDOut = new DataOutputStream(mSocket.getOutputStream());
                byte[] netID = {-1};
                mBIn.read(netID);
                mClientListener.connectedToServer((int) netID[0]);
            } catch (UnknownHostException exception) {
                mOpen = false;
                mClientListener.unknownHost();
            } catch (IOException exception) {
                mOpen = false;
                mClientListener.couldNotConnect();
            }

            byte[] bArray;
            byte[] terminateBytes =
                    new byte[NetworkConfig.TERMINATE_BYTES_LENGTH]; // max flatbuffer size
            while (mOpen) {
                try {
                    bArray = NetworkMessage.readMessageFromStream(mBIn);
                    if (bArray.length != 0) {
                        if (Arrays.equals(bArray, terminateBytes)) {
                            break;
                        } else {
                            queueRequest(bArray);
                        }
                    }

                } catch (IOException ignore) { // if fails to read from in stream
                    if (mClientListener != null) {
                        mClientListener.error("failed to read from input stream");
                    }
                    break;
                }
            }

            if (mClientListener != null) mClientListener.disconnected();

            dispose();
            System.out.println("cancelled successfully");
        }
    }

    /**
     * Queue a new request for the client to process.
     *
     * @param bArray the bytes
     */
    private void queueRequest(byte[] bArray) {
        mLogger.fine("queuing request :: " + Hex.encodeHexString(bArray));
        this.mRequests.add(bArray);
    }

    /** Processes all requests. */
    public int processRequests() {
        mLogger.fine("processing all " + this.mRequests.size() + " requests");
        int cnt = 0;
        while (!this.mRequests.isEmpty()) {
            byte[] requestBytes = this.mRequests.poll();
            if (requestBytes != null) {
                parseBytes(requestBytes);
            }
            if (cnt > 100) mLogger.info("CNT " + cnt);

            cnt++;
        }
        return cnt;
    }

    /**
     * Parses bytes from a message.
     *
     * @param bytes the bytes
     * @throws DecodingException the decoding exception
     */
    private void parseBytes(byte[] bytes) {
        try {
            NetworkMessage.parse(bytes, this);
        } catch (Exception e) {
            mLogger.fine("error parsing bytes");
            e.printStackTrace();
        }
    }

    /** Closes all connections. */
    private void closeAllConnections() {
        mOpen = false;

        try {
            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        try {
            if (mDOut != null) {
                mDOut.close();
                mDOut = null;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
