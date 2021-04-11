/* (C) 2021 DragonSkulle */

package org.dragonskulle.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

/**
 * Stores server's client connection.
 *
 * @author Aurimas Blažulionis
 * @author Oscar L
 */
@Log
@Accessors(prefix = "m")
public class ServerClient {
    /**
     * Network ID. All networked clients will have a non-negative ID. Negative IDs indicate either
     * invalid IDs, or server owned objects
     */
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private int mNetworkID = -1;

    /** Underlying {@link Socket}. */
    private Socket mSocket;
    /** Is the client loop running, and supposed to be running. */
    @Getter private boolean mRunning;
    /** Reference to the server event listener. */
    private IServerListener mServerListener;
    /** Thread of the input loop. */
    private Thread mThread;
    /** Output stream for the socket. */
    @Getter private DataOutputStream mDataOut;

    private TimeoutInputStream mTimeoutInputStream;
    /** The scheduled requests to be processed. */
    private DataInputStream mInput;

    /**
     * Constructor for {@link ServerClient}.
     *
     * @param socket socket for this connection
     * @param serverListener reference to the server listener
     */
    ServerClient(Socket socket, IServerListener serverListener) {
        mSocket = socket;
        mServerListener = serverListener;
    }

    /**
     * Process a number of requests.
     *
     * <p>This method will process up to the specified number of requests, and return the number of
     * requests actually processed.
     *
     * @param count maximum number of requests to process
     * @return number of requests processed
     */
    public int processRequests(int count) {
        int i = 0;

        try {
            int oldSoTime = mSocket.getSoTimeout();
            mSocket.setSoTimeout(mTimeoutInputStream.getTimeout());

            for (i = 0; i < count && mInput.available() > 0; i++) {
                mTimeoutInputStream.enableTimeout();
                parseRequest();
            }

            mTimeoutInputStream.disableTimeout();
            mSocket.setSoTimeout(oldSoTime);
        } catch (IOException e) {
            e.printStackTrace();
            closeSocket();
        }

        return i;
    }

    /**
     * Send byte message to the client.
     *
     * @param message message to send
     */
    public void sendBytes(byte[] message) {
        try {
            mDataOut.write(message);
            mDataOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
            mThread.interrupt();
        }
    }

    /** Close the socket, tell the thread to stop. */
    public void closeSocket() {
        try {
            mDataOut.writeByte(NetworkConfig.Codes.MESSAGE_DISCONNECT);
            mDataOut.flush();
        } catch (Exception ignored) {

        }

        try {
            mRunning = false;
            mSocket.shutdownOutput();
            mSocket.close();
        } catch (IOException ignored) {
        }
    }

    /** Join the underlying thread. */
    void joinThread() {
        try {
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /** Start the network input thread. */
    void startThread() {
        mThread = new Thread(this::run);
        mThread.setDaemon(true);
        mThread.setName("Client " + mSocket.getInetAddress().toString());
        mThread.start();
    }

    /**
     * THe Client Runner is the thread given to each client to handle its own socket. Commands are
     * read from the input stream. It will pass all commands to the correct handler function. {@link
     * org.dragonskulle.network.IServerListener}***
     */
    private void run() {
        mRunning = true;

        try {
            log.info("Spawned client thread");

            BufferedInputStream bIn = new BufferedInputStream(mSocket.getInputStream());
            mTimeoutInputStream = new TimeoutInputStream(bIn, 100);
            mInput = new DataInputStream(mTimeoutInputStream);
            mDataOut = new DataOutputStream(new BufferedOutputStream(mSocket.getOutputStream()));

            mServerListener.clientConnected(this);

            log.info("Got ID " + mNetworkID);

            if (mNetworkID == -1) {
                closeSocket();
            }

            while (mRunning && mSocket.isConnected() && !mSocket.isClosed()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }

            closeSocket();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        triggerDisconnect();
    }

    private void triggerDisconnect() {
        if (mRunning) {
            mRunning = false;
            mServerListener.clientDisconnected(this);
        }
    }

    /**
     * Parses a network message from bytes and executes the correct functions. This is for server
     * use.
     *
     */
    private void parseRequest() throws IOException {
        byte messageType = mInput.readByte();

        dispatchMessage(messageType);
    }

    /**
     * Executes bytes on the server.
     *
     * @param messageType the message type
     */
    private void dispatchMessage(byte messageType) throws IOException {
        switch (messageType) {
            case NetworkConfig.Codes.MESSAGE_DISCONNECT:
                triggerDisconnect();
                mThread.interrupt();
                break;
            case NetworkConfig.Codes.MESSAGE_CLIENT_REQUEST:
                handleClientRequest();
                break;
            default:
                log.info("The server received invalid request: " + messageType);
                break;
        }
    }

    private void handleClientRequest() throws IOException {
        int objectID = mInput.readInt();
        int requestID = mInput.readInt();

        mServerListener.clientComponentRequest(this, objectID, requestID, mInput);
    }
}
