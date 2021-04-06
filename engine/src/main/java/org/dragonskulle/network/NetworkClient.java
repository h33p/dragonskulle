/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

/**
 * @author Oscar L
 *     <p>This is the client usage, you will create an instance, by providing the correct server to
 *     connect to. ClientListener is the handler for commands that the client receives. {@link
 *     org.dragonskulle.network.IClientListener}**
 */
@Log
@Accessors(prefix = "m")
public class NetworkClient {

    /** The Socket connection to the server. */
    private Socket mSocket;
    /** The byte output stream. */
    @Getter private DataOutputStream mDataOut;
    /** The byte input stream. */
    private BufferedInputStream mBIn;

    /** The Client listener to notify of important events. */
    private IClientListener mClientListener;
    /** True if the socket is open. */
    private boolean mOpen = true;

    /** The Runnable for @{mClientThread}. */
    private ClientRunner mClientRunner;

    /** The thread that watches @link{dIn} for messages. */
    private Thread mClientThread;

    private AtomicBoolean didDispose = new AtomicBoolean(false);

    private TimeoutInputStream mTimeoutInputStream;
    /** The input stream from the server */
    private DataInputStream mInput;

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
            if (!didDispose.get()) {
                didDispose.set(true);
                if (mOpen) {
                    mOpen = false;
                    closeAllConnections();
                    if (mClientListener != null) {
                        mClientListener.disconnected();
                    }
                }
                if (mSocket != null) mSocket.close();
                if (mClientThread != null) {
                    mClientThread.interrupt();
                    try {
                        mClientThread.join();
                    } catch (InterruptedException e) {

                    }
                }
                mSocket = null;
                mDataOut = null;
                mClientListener = null;
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
                if (mDataOut != null) {
                    log.fine("sending bytes");
                    mDataOut.write(bytes);
                }
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
    @SuppressWarnings("")
    private class ClientRunner implements Runnable {
        private String mIP;
        private int mPort;

        @Override
        public void run() {
            try {
                mSocket = new Socket(mIP, mPort);
                mDataOut = new DataOutputStream(mSocket.getOutputStream());
                byte[] netID = {-1};
                mBIn = new BufferedInputStream(mSocket.getInputStream());
                mTimeoutInputStream = new TimeoutInputStream(mBIn, 500);
                mBIn.read(netID);
                mInput = new DataInputStream(mTimeoutInputStream);
                mClientListener.connectedToServer((int) netID[0]);
            } catch (UnknownHostException exception) {
                mOpen = false;
                mClientListener.unknownHost();
            } catch (IOException exception) {
                mOpen = false;
                mClientListener.couldNotConnect();
            }

            while (mOpen && mSocket.isConnected()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }

            if (mClientListener != null) mClientListener.disconnected();

            dispose();
            log.info("cancelled successfully");
        }
    }

    /** Processes all requests. */
    public int processRequests() {
        log.fine("processing all requests");
        int cnt = 0;

        try {
            int oldSoTime = mSocket.getSoTimeout();
            mSocket.setSoTimeout(mTimeoutInputStream.getTimeout());

            while (mInput.available() > 0) {
                mTimeoutInputStream.enableTimeout();
                processMessage();

                if (cnt > 100) log.info("CNT " + cnt);

                cnt++;
            }

            mTimeoutInputStream.disableTimeout();
            mSocket.setSoTimeout(oldSoTime);
        } catch (IOException e) {
            e.printStackTrace();
            closeAllConnections();
        }

        return cnt;
    }

    /**
     * Process a message
     *
     * @return the byteCode of the message processed.
     */
    private byte processMessage() throws IOException {
        byte messageType = mInput.readByte();
        log.fine("EXEB - " + messageType);
        switch (messageType) {
            case NetworkConfig.Codes.MESSAGE_DISCONNECT:
                mClientThread.interrupt();
                break;
            case NetworkConfig.Codes.MESSAGE_UPDATE_OBJECT:
                log.fine("Should update requested network object");
                mClientListener.updateNetworkObject(mInput);
                break;
            case NetworkConfig.Codes.MESSAGE_SPAWN_OBJECT:
                log.fine("Spawn a networked object");
                mClientListener.spawnNetworkObject(mInput);
                break;
            case NetworkConfig.Codes.MESSAGE_UPDATE_STATE:
                log.fine("Update server's state");
                mClientListener.updateServerState(mInput);
                break;
            case NetworkConfig.Codes.MESSAGE_SERVER_EVENT:
                log.fine("A server object event");
                mClientListener.objectEvent(mInput);
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

        try {
            mDataOut.writeByte(NetworkConfig.Codes.MESSAGE_DISCONNECT);
            mDataOut.flush();
        } catch (Exception e) {

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
