/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import com.sun.xml.internal.org.jvnet.mimepull.DecodingException;
import org.dragonskulle.network.components.sync.SyncVar;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This is the client usage, you will create an instance, by providing the correct server to connect
 * to. ClientListener is the handler for commands that the client receives. {@link
 * org.dragonskulle.network.ClientListener}
 */
public class NetworkClient {
    private static final int MAX_TRANSMISSION_SIZE = 512;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private DataOutputStream dOut;
    private BufferedInputStream bIn;
    private ArrayList<SyncVar> synced = new ArrayList<>();


    private ClientListener clientListener;
    private boolean open = true;

    public NetworkClient(String ip, int port, ClientListener listener) {
        clientListener = listener;
        try {
            socket = new Socket(ip, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bIn = new BufferedInputStream(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);
            dOut = new DataOutputStream(socket.getOutputStream());
            Thread clientThread = new Thread(this.clientRunner());
            clientThread.setName("Client Connection");
            clientThread.setDaemon(true);
            clientThread.start();
            listener.connectedToServer();
        } catch (UnknownHostException exception) {
            open = false;
            listener.unknownHost();
        } catch (IOException exception) {
            open = false;
            listener.couldNotConnect();
        } catch (Exception exception) {
            open = false;
            exception.printStackTrace();
        }
    }

    public void dispose() {
        try {
            if (open) {
                this.sendBytes(new byte[MAX_TRANSMISSION_SIZE]);
                open = false;
                closeAllConnections();
                clientListener.disconnected();
            }
            socket = null;
            in = null;
            out = null;
            dOut = null;
            clientListener = null;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void send(String msg) {
        this.sendBytes(msg.getBytes());
    }

    public void sendBytes(byte[] bytes) {
        if (open) {
            try {
                System.out.println("sending bytes");
                dOut.write(bytes);
            } catch (IOException e) {
                System.out.println("Failed to send bytes");
            }
        }
    }

    public boolean isConnected() {
        return open;
    }

    /**
     * This is the thread which is created once the connection is achieved. It is used to handle
     * messages received from the server. It also handles the server disconnection.
     *
     * @return
     */
    private Runnable clientRunner() {

        return () -> {
            byte[] bArray;
            int hasBytes = 0;
            byte[] terminateBytes = new byte[MAX_TRANSMISSION_SIZE]; //max flatbuffer size
            while (open) {
                try {
                    bArray = NetworkMessage.readMessageFromStream(bIn);
                    if (bArray.length != 0) {
                        if (Arrays.equals(bArray, terminateBytes)) {
                            clientListener.disconnected();
                            this.dispose();
                            break;
                        } else {
                            processBytes(bArray);
                        }
                    }

                } catch (IOException ignore) { // if fails to read from in stream
                    clientListener.error("failed to read from input stream");
                    this.dispose();
                    break;
                }
            }
        };
    }

    private void processBytes(byte[] bytes) {
        clientListener.receivedBytes(bytes);

        try {
            parseBytes(bytes);
        } catch (DecodingException e) {
            System.out.println(e.getMessage());
            System.out.println(new String(bytes, StandardCharsets.UTF_8));
        }

    }

    private void parseBytes(byte[] bytes) throws DecodingException {
        System.out.println("bytes unpacking");
        try {
            NetworkMessage.parse(bytes);
        } catch (Exception e) {

            throw new DecodingException("Message is not of valid type");
        }
    }


    private void closeAllConnections() {
        open = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        try {
            if (in != null) {
                in.close();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        try {
            if (dOut != null) {
                dOut.close();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
