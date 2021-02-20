/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import com.sun.xml.internal.org.jvnet.mimepull.DecodingException;
import org.dragonskulle.network.flatbuffers.FlatBufferHelpers;
import org.dragonskulle.network.proto.ISyncVar;
import org.dragonskulle.network.proto.RegisteredSyncVarsResponse;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
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
    private ArrayList<org.dragonskulle.network.components.ISyncVar> synced = new ArrayList<>();


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
                System.out.println(Arrays.toString(bytes));
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
                    bArray = new byte[MAX_TRANSMISSION_SIZE];
                    hasBytes = bIn.read(bArray);

                    if (hasBytes != 0) {
                        if (Arrays.equals(bArray, terminateBytes)) {
                            clientListener.disconnected();
                            this.dispose();
                            break;
                        } else {
                            parseBytes(bArray);
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

    private void parseBytes(byte[] bytes) {
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(bytes);
        clientListener.receivedBytes(bytes);

//        decode bytes from flatbuffer serialisation
//        currently only one type;
        try {
            ArrayList<org.dragonskulle.network.components.ISyncVar> registered = parseRegisterSyncVarsResponse(buf);
            updateSyncedVariables(registered);
        } catch (DecodingException e) {
            System.out.println(e.getMessage());
            System.out.println(new String(bytes, StandardCharsets.UTF_8));
        }

    }

    private void updateSyncedVariables(ArrayList<org.dragonskulle.network.components.ISyncVar> registered) {
        synced.addAll(registered);
        System.out.println("Registered all accepted syncs locally and on server!");
    }

    private ArrayList<org.dragonskulle.network.components.ISyncVar> parseRegisterSyncVarsResponse(ByteBuffer buf) throws
            DecodingException {
        System.out.println("Attempting to parse response");
        ArrayList<org.dragonskulle.network.components.ISyncVar> syncsToAdd = new ArrayList<>();
        try {
            RegisteredSyncVarsResponse registeredSyncVarsResponse = RegisteredSyncVarsResponse.getRootAsRegisteredSyncVarsResponse(buf);
            System.out.println("Contains number of successfully sync vars: " + registeredSyncVarsResponse.registeredLength());
            for (int i = 0; i < registeredSyncVarsResponse.registeredLength(); i++) {
                org.dragonskulle.network.proto.ISyncVar requestedSyncVar = registeredSyncVarsResponse.registeredVector().get(i);
                org.dragonskulle.network.components.ISyncVar sync = FlatBufferHelpers.flatb2ISyncVar(requestedSyncVar);
                syncsToAdd.add(sync);
            }

            return syncsToAdd;

        } catch (
                Exception e) {
            throw new DecodingException("Is not of RegisterSyncVarsRequest Type");
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

    //    public void registerSyncVarWithServer(ISyncVar var, Boolean isDormant) {
//
//        String jsonStr = null;
//        try {
//            jsonStr = new ObjectMapper().writeValueAsString(msg);
//            this.out.write(jsonStr);
//            this.out.flush();
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//        }
//    }
    public void registerSyncVarsWithServer(byte[] packet) {
        System.out.println("registering");
        System.out.println("Preparing packet to be sent");
        this.sendBytes(packet);
        System.out.println("Sent registration request, should validate response before adding to synced vars in network object");
    }
}
