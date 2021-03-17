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
    private int mNetworkID = -1;

    private Socket mSocket;
    private boolean mRunning;
    private ServerListener mServerListener;
    private Thread mThread;
    private DataOutputStream mDataOut;

    /** The scheduled requests to be processed. */
    private final ListenableQueue<byte[]> mRequests = new ListenableQueue<>(new LinkedList<>());

    public ServerClient(Socket socket, ServerListener serverListener) {
        mSocket = socket;
        mServerListener = serverListener;
    }

    public int processRequests(int count) {
        int i = 0;
        byte[] req = null;
        for (i = 0; (req = mRequests.poll()) != null && i < count; i++) parse(req);
        return i;
    }

    public void closeSocket() {
        try {
            mRunning = false;
            mSocket.shutdownOutput();
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void joinThread() {
        try {
            mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void startThread() {
        mThread = new Thread(this::run);
        mThread.setDaemon(true);
        mThread.setName("Client " + mSocket.getInetAddress().toString());
        mThread.start();
    }

    public void sendBytes(byte[] message) {
        try {
            mDataOut.write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * THe Client Runner is the thread given to each client to handle its own socket. Commands are
     * read from the input stream. It will pass all commands to the correct handler function. {@link
     * org.dragonskulle.network.ServerListener}***
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
                    executeBytes(messageType, payload);
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
    private void executeBytes(byte messageType, byte[] payload) {
        log.info("EXECB - " + messageType);
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
