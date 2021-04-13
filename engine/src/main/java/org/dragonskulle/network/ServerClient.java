/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.dragonskulle.utils.IOUtils;

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
     * invalid IDs, or server owned objects.
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
    private DataOutputStream mDataOut;

    /** The scheduled requests to be processed. */
    private final ConcurrentLinkedQueue<byte[]> mRequests = new ConcurrentLinkedQueue<>();

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
     * Creates a new NetworkMessageStream from the outputstream on the server socket.
     *
     * @return the new stream
     */
    public DataOutputStream getDataOut() {
        return new NetworkMessageStream(mDataOut);
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

        byte[] req = null;

        for (i = 0; i < count && (req = mRequests.poll()) != null; i++) {
            try {
                ByteArrayInputStream bin = new ByteArrayInputStream(req);
                DataInputStream stream = new DataInputStream(bin);
                parseRequest(stream);
                stream.close();
                bin.close();
            } catch (IOException e) {
                e.printStackTrace();
                // closeAllConnections();
            }
        }

        return i;
    }

    /**
     * Send byte message to the client.
     *
     * @param message message to send
     */
    public void sendBytes(byte[] message) throws IOException {
        mDataOut.writeShort((short) message.length);
        mDataOut.write(message);
        mDataOut.flush();
    }

    /** Close the socket, tell the thread to stop. */
    public void closeSocket() {
        try (DataOutputStream dataOut = getDataOut()) {
            dataOut.writeByte(NetworkConfig.Codes.MESSAGE_DISCONNECT);
        } catch (Exception ignored) {

        }

        try {
            triggerDisconnect();
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
            DataInputStream input = new DataInputStream(bIn);
            mDataOut = new DataOutputStream(new BufferedOutputStream(mSocket.getOutputStream()));

            mServerListener.clientConnected(this);

            if (mNetworkID == -1) {
                closeSocket();
            }

            mDataOut.writeByte((byte) mNetworkID);

            while (mRunning && mSocket.isConnected() && !mSocket.isClosed()) {
                short len = input.readShort();
                byte[] bytes = IOUtils.readExactlyNBytes(input, len);
                mRequests.add(bytes);
            }
        } catch (EOFException ignored) {
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            closeSocket();
        }

        triggerDisconnect();
    }

    /** Start disconnecting the client. */
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
     * @param stream stream to parse the request from
     */
    private void parseRequest(DataInputStream stream) throws IOException {
        byte messageType = stream.readByte();

        switch (messageType) {
            case NetworkConfig.Codes.MESSAGE_DISCONNECT:
                triggerDisconnect();
                mThread.interrupt();
                break;
            case NetworkConfig.Codes.MESSAGE_CLIENT_REQUEST:
                handleClientRequest(stream);
                break;
            default:
                log.info("The server received invalid request: " + messageType);
                break;
        }
    }

    /**
     * Handle a client request for an object.
     *
     * @param stream the stream
     * @throws IOException the io exception
     */
    private void handleClientRequest(DataInputStream stream) throws IOException {
        int objectID = stream.readInt();
        int requestID = stream.readInt();

        mServerListener.clientComponentRequest(this, objectID, requestID, stream);
    }
}
