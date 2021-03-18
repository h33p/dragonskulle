/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

/**
 * Stores server's client connection
 *
 * @author Aurimas Blažulionis
 * @author Oscar L
 */
@Log
@Accessors(prefix = "m")
public class ServerClient {
    @Getter
    @Setter(AccessLevel.PACKAGE)
    /**
     * Network ID. All networked clients will have a non-negative ID. Negative IDs indicate either
     * invalid IDs, or server owned objects
     */
    private int mNetworkID = -1;

    /** Underlying {@link Socket} */
    private Socket mSocket;
    /** Is the client loop running, and supposed to be running */
    private boolean mRunning;
    /** Reference to the server event listener */
    private IServerListener mServerListener;
    /** Thread of the input loop */
    private Thread mThread;
    /** Output stream for the socket */
    private DataOutputStream mDataOut;

    /** The scheduled requests to be processed. */
    private final ListenableQueue<byte[]> mRequests = new ListenableQueue<>(new LinkedList<>());

    /**
     * Constructor for {@link ServerClient}
     *
     * @param socket socket for this connection
     * @param serverListener reference to the server listener
     */
    ServerClient(Socket socket, IServerListener serverListener) {
        mSocket = socket;
        mServerListener = serverListener;
    }

    /**
     * Process a number of requests
     *
     * <p>This method will process up to the specified number of requests, and return the number of
     * requests actually processed
     *
     * @param count maximum number of requests to process
     * @return number of requests processed
     */
    public int processRequests(int count) {
        int i = 0;
        byte[] req = null;
        for (i = 0; (req = mRequests.poll()) != null && i < count; i++) parse(req);
        return i;
    }

    /**
     * Send byte message to the client
     *
     * @param message message to send
     */
    public void sendBytes(byte[] message) {
        try {
            mDataOut.write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Close the socket, tell the thread to stop */
    void closeSocket() {
        try {
            mRunning = false;
            mSocket.shutdownOutput();
            mSocket.close();
        } catch (IOException e) {
        }
    }

    /** Join the underlying thread */
    void joinThread() {
        try {
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /** Start the network input thread */
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
            byte[] bArray; // max flatbuffer size
            byte[] terminateBytes =
                    new byte[NetworkConfig.TERMINATE_BYTES_LENGTH]; // max flatbuffer size

            BufferedInputStream bIn = new BufferedInputStream(mSocket.getInputStream());
            mDataOut = new DataOutputStream(mSocket.getOutputStream());

            mServerListener.clientConnected(this);

            log.info("Got ID " + mNetworkID);

            if (mNetworkID == -1) closeSocket();

            try {
                while (mRunning && mSocket.isConnected()) {
                    bArray = NetworkMessage.readMessageFromStream(bIn);
                    if (bArray.length != 0) {
                        if (Arrays.equals(bArray, terminateBytes)) {
                            break;
                        } else {
                            mRequests.add(bArray);
                        }
                    }
                }
            } catch (IOException e) {
            }

            closeSocket();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        mRunning = false;

        mServerListener.clientDisconnected(this);
    }

    /**
     * Parses a network message from bytes and executes the correct functions. This is for server
     * use.
     *
     * @param buff the buff
     */
    private void parse(byte[] buff) {
        if (buff.length == 0 || Arrays.equals(buff, new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0})) {
            return;
        }
        int i = 0;
        boolean validStart = NetworkMessage.verifyMessageStart(buff);
        i += 5;
        if (validStart) {
            byte messageType = NetworkMessage.getMessageType(buff);
            i += 1;
            int payloadSize = NetworkMessage.getPayloadSize(buff);
            i += 4;
            byte[] payload = NetworkMessage.getPayload(buff, messageType, i, payloadSize);
            i += payloadSize;
            boolean consumedMessage = NetworkMessage.verifyMessageEnd(i, buff);
            if (consumedMessage) {
                if (messageType == (byte) 0) {
                    log.fine("\nValid Message");
                    log.fine("Type : " + messageType);
                    log.fine("Payload : " + Arrays.toString(payload));
                } else {
                    dispatchMessage(messageType, payload);
                }
            }
        } else {
            log.fine("invalid message start");
        }
    }

    /**
     * Executes bytes on the server.
     *
     * @param messageType the message type
     * @param payload the payload
     */
    private void dispatchMessage(byte messageType, byte[] payload) {
        switch (messageType) {
            case NetworkConfig.Codes.MESSAGE_CLIENT_REQUEST:
                handleClientRequest(payload);
                break;
            default:
                log.info("The server received invalid request: " + messageType);
                break;
        }
    }

    private void handleClientRequest(byte[] payload) {
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(payload)) {
            try (DataInputStream stream = new DataInputStream(bytes)) {
                int objectID = stream.readInt();
                int requestID = stream.readInt();

                mServerListener.clientComponentRequest(this, objectID, requestID, stream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
