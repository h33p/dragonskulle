/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import com.google.flatbuffers.FlatBufferBuilder;
import com.sun.xml.internal.org.jvnet.mimepull.DecodingException;
import org.dragonskulle.network.components.ISyncVar;
import org.dragonskulle.network.flatbuffers.FlatBufferHelpers;
import org.dragonskulle.network.proto.*;

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
        clientListener.receivedBytes(bytes);

        try {
            parseFlatBufferBytes(bytes);

        } catch (DecodingException e) {
            System.out.println(e.getMessage());
            System.out.println(new String(bytes, StandardCharsets.UTF_8));
        }

    }

    private void parseFlatBufferBytes(byte[] bytes) throws DecodingException {
        System.out.println("flatbuffer parsing");
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(bytes);
        Message message = org.dragonskulle.network.proto.Message.getRootAsMessage(buf);
        byte contentType = message.contentsType();
        if (contentType == VariableMessage.RegisteredSyncVarsResponse) {
            System.out.println("request is of type RegisterSyncVarsResponse");
            RegisteredSyncVarsResponse registeredResponse = (RegisteredSyncVarsResponse) message.contents(new RegisteredSyncVarsResponse());
            assert registeredResponse != null;
            ArrayList<org.dragonskulle.network.components.ISyncVar> registered = parseRegisterSyncVarsResponse(registeredResponse);
            updateRegisteredSyncedVariables(registered);
        } else if (contentType == VariableMessage.UpdateSyncVarsResponse) {
            System.out.println("request is of type UpdateSyncVarsResponse");
            UpdateSyncVarsResponse updateResponse = (UpdateSyncVarsResponse) message.contents(new UpdateSyncVarsResponse());
            assert updateResponse != null;
            parseUpdateSyncVarsResponse(updateResponse);
        } else {
            throw new DecodingException("Message is not of valid type");
        }
    }

    private void parseUpdateSyncVarsResponse(UpdateSyncVarsResponse updateResponse) {
        System.out.println("received request to update local copy of a variable");
        assert updateResponse != null;
        ISyncVar newSyncValue = FlatBufferHelpers.flatb2ISyncVar(updateResponse.syncVar());
        ISyncVar var;
        for (int i = 0; i < this.synced.size(); i++) {
            var = this.synced.get(i);
            if (var.equals(newSyncValue)) {
                newSyncValue.attachParent(var.getParent());
                this.synced.set(i, newSyncValue);
                System.out.println("performed update request");
            }
        }
    }


    private void updateRegisteredSyncedVariables(ArrayList<org.dragonskulle.network.components.ISyncVar> registered) {
        for (ISyncVar sync : registered) {
            sync.attachUpdateListener(this::requestServerUpdateSyncVar);
            synced.add(sync); //do we need to do this or can we just store ids?
        }
        System.out.println("Registered all accepted syncs locally and on server!");
    }

    public void requestServerUpdateSyncVar(String netId, ISyncVar newValue) {
        System.out.println("updater function for sync var, should request from server");
        //TODO need to consider if dormant server side.
        FlatBufferBuilder builder = new FlatBufferBuilder();
        int netIdOffset = builder.createString(netId);
        int syncVarOffset = FlatBufferHelpers.ISyncVar2flatb(builder, newValue);
        int requestOffset = UpdateSyncVarsRequest.createUpdateSyncVarsRequest(builder, netIdOffset, syncVarOffset);

        int messageOffset = Message.createMessage(builder, VariableMessage.UpdateSyncVarsRequest, requestOffset);
        builder.finish(messageOffset);
        System.out.println("requesting update to server of altered syncvar");
        this.sendBytes(builder.sizedByteArray());
    }

    private ArrayList<org.dragonskulle.network.components.ISyncVar> parseRegisterSyncVarsResponse(RegisteredSyncVarsResponse contents) throws
            DecodingException {
        System.out.println("Attempting to parse response");
        ArrayList<org.dragonskulle.network.components.ISyncVar> syncsToAdd = new ArrayList<>();
        try {
            System.out.println("Contains number of successfully sync vars: " + contents.registeredLength());
            for (int i = 0; i < contents.registeredLength(); i++) {
                org.dragonskulle.network.proto.ISyncVar requestedSyncVar = contents.registeredVector().get(i);
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


    public void registerSyncVarsWithServer(byte[] packet) {
        System.out.println("registering");
        System.out.println("Preparing packet to be sent");
        this.sendBytes(packet);
        System.out.println("Sent registration request, should validate response before adding to synced vars in network object");
    }

    public void notifySyncedOfUpdate(String netId, boolean isNetObjectDormant, ISyncVar newData) {
        System.out.println("notifySyncedOfUpdate");
        System.out.println("looking for :: " + newData.getId());
        for (ISyncVar iSyncVar : this.synced) {
            System.out.println("cmp->" + iSyncVar.getId());
            if (iSyncVar.equals(newData)) {
                System.out.println("found synced in networkclient list");
                iSyncVar.runUpdateCallback(netId, newData);
                break;
            }
        }
    }

    public Object getSynced(String id) {
        for (ISyncVar iSyncVar : this.synced) {
            if(iSyncVar.getId().equals(id)){
                return iSyncVar.looselyGet();
            }
        }
        return null;
    }
}
