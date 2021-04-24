/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.utils.IOUtils;

/**
 * This is the client usage, you will create an instance, by providing the correct server to connect
 * to. ClientListener is the handler for commands that the client receives. {@link
 * org.dragonskulle.network.IClientListener}.
 *
 * @author Oscar L
 */
@Log
@Accessors(prefix = "m")
public class NetworkClient {

    /** The Socket connection to the server. */
    private Socket mSocket;
    /** The byte output stream. */
    private DataOutputStream mDataOut;
    /** The byte input stream. */
    private BufferedInputStream mBIn;

    /** The Client listener to notify of important events. */
    private final IClientListener mClientListener;
    /** True if the socket is open. */
    private boolean mOpen = true;

    /** The Runnable for @{mClientThread}. */
    private ClientRunner mClientRunner;

    /** The thread that watches @link{dIn} for messages. */
    private Thread mClientThread;

    private final AtomicBoolean mDidDispose = new AtomicBoolean(false);

    /** Stores all requests from the server once scheduled. */
    private final ConcurrentLinkedQueue<byte[]> mRequests = new ConcurrentLinkedQueue<>();

    public DataOutputStream getDataOut() {
        return new NetworkMessageStream(mDataOut);
    }

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
            log.severe(exception.getMessage());
        }
    }

    /** Dispose. */
    public void dispose() {
        try {
            if (!mDidDispose.get()) {
                mDidDispose.set(true);
                if (mOpen) {
                    mOpen = false;
                    closeAllConnections();
                    if (mClientListener != null) {
                        mClientListener.disconnected();
                    }
                }
                if (mSocket != null) {
                    mSocket.close();
                }
                if (mClientThread != null) {
                    mClientThread.interrupt();
                    try {
                        mClientThread.join();
                    } catch (InterruptedException ignored) {

                    }
                }
                mSocket = null;
                mDataOut = null;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            log.severe(exception.getMessage());
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
                mDataOut.writeShort(bytes.length);
                mDataOut.write(bytes);
            } catch (IOException e) {
                log.fine("Failed to send bytes");
                mClientThread.interrupt();
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
    private class ClientRunner implements Runnable {
        private String mIP;
        private int mPort;

        @Override
        public void run() {
            try {
                mSocket = new Socket();
                mSocket.connect(new InetSocketAddress(mIP, mPort), 1000);
                mDataOut = new DataOutputStream(mSocket.getOutputStream());
                mBIn = new BufferedInputStream(mSocket.getInputStream());
                DataInputStream input = new DataInputStream(mBIn);
                byte netID = input.readByte();
                mClientListener.connectedToServer(netID);

                while (mOpen && mSocket.isConnected()) {
                    try {
                        short len = input.readShort();
                        byte[] bytes = IOUtils.readExactlyNBytes(input, len);
                        mRequests.add(bytes);
                    } catch (IOException e) {
                        break;
                    }
                }
            } catch (UnknownHostException exception) {
                mOpen = false;
                mClientListener.unknownHost();
            } catch (IOException exception) {
                mOpen = false;
                mClientListener.couldNotConnect();
            }

            if (mClientListener != null) {
                mClientListener.disconnected();
            }

            dispose();
            log.info("cancelled successfully");
        }
    }

    /** Processes all requests. */
    public int processRequests() {

        if (mDidDispose.get()) {
            return 0;
        }

        log.fine("processing all " + this.mRequests.size() + " requests");
        int cnt = 0;

        while (!this.mRequests.isEmpty()) {
            byte[] requestBytes = mRequests.poll();
            if (requestBytes != null) {
                try {
                    ByteArrayInputStream bin = new ByteArrayInputStream(requestBytes);
                    DataInputStream stream = new DataInputStream(bin);
                    processMessage(stream);
                    stream.close();
                    bin.close();
                    cnt++;
                } catch (IOException e) {
                    e.printStackTrace();
                    // closeAllConnections();
                }
            }
        }

        return cnt;
    }

    /**
     * Process a message.
     *
     * @param stream stream to read the message from
     * @return the byteCode of the message processed.
     */
    private byte processMessage(DataInputStream stream) throws IOException {
        byte messageType = stream.readByte();
        log.fine("EXEB - " + messageType);
        switch (messageType) {
            case NetworkConfig.Codes.MESSAGE_DISCONNECT:
                mClientThread.interrupt();
                break;
            case NetworkConfig.Codes.MESSAGE_UPDATE_OBJECT:
                log.fine("Should update requested network object");
                mClientListener.updateNetworkObject(stream);
                break;
            case NetworkConfig.Codes.MESSAGE_SPAWN_OBJECT:
                log.fine("Spawn a networked object");
                mClientListener.spawnNetworkObject(stream);
                break;
            case NetworkConfig.Codes.MESSAGE_UPDATE_STATE:
                log.fine("Update server's state");
                mClientListener.updateServerState(stream);
                break;
            case NetworkConfig.Codes.MESSAGE_SERVER_EVENT:
                log.fine("A server object event");
                mClientListener.objectEvent(stream);
                break;
            default:
                log.info("unsure of what to do with message as unknown type byte " + messageType);
                break;
        }
        return messageType;
    }

    /** Closes all connections. */
    private void closeAllConnections() {
        mOpen = false;

        try (DataOutputStream dataOut = getDataOut()) {
            dataOut.writeByte(NetworkConfig.Codes.MESSAGE_DISCONNECT);
        } catch (Exception ignored) {

        }

        try {
            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
        } catch (Exception exception) {
            log.severe(exception.getMessage());
        }
        try {
            if (mDataOut != null) {
                mDataOut.close();
                mDataOut = null;
            }
        } catch (Exception exception) {
            log.severe(exception.getMessage());
        }
    }
}
