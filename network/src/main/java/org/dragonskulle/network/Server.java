/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;
// based on
// https://github.com/TheDudeFromCI/WraithEngine/tree/5397e2cfd75c257e4d96d0fd6414e302ab22a69c/WraithEngine/src/wraith/library/Multiplayer

import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.Table;
import com.sun.javafx.collections.ImmutableObservableList;
import com.sun.xml.internal.org.jvnet.mimepull.DecodingException;
import org.dragonskulle.network.components.*;
import org.dragonskulle.network.components.ISyncVar;
import org.dragonskulle.network.flatbuffers.FlatBufferHelpers;
import org.dragonskulle.network.proto.*;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * This is the main Server Class, it handles setup and stores all client connections. It can
 * broadcast messages to every client and receive from individual clients.
 */
public class Server {
    private int port;
    private ServerListener serverListener;
    private final SocketStore sockets = new SocketStore();
    private Thread serverThread;
    private ServerRunner serverRunner;
    private static final SyncStore syncedVars = new SyncStore();

    public Server(int port, ServerListener listener) {
        System.out.println("[S] Setting up server");
        serverListener = listener;
        try {
            ServerSocket server_sock =
                    new ServerSocket(port, 0, InetAddress.getByName(null)); // sets up on localhost
            sockets.initServer(server_sock);
            if (this.port == 0) {
                this.port = sockets.getServerPort();
            } else {
                this.port = port;
            }
            serverRunner = new ServerRunner();
            serverThread = new Thread(this.serverRunner);
            serverThread.setDaemon(true);
            serverThread.setName("Server");
            System.out.println("[S] Starting server");
            serverThread.start();

            String command;
            Scanner scanner = new Scanner(System.in);
            String[] input;
            OUTER_LOOP:
            while (true) {
                System.out.println("Enter Command: (B)roadcast -s {message} | (K)ill");
                command = scanner.nextLine();
                input = command.split(" -s ");
                switch (input[0].toUpperCase()) {
                    case ("B"):
                        try {
                            System.out.println("Broadcasting {" + input[1] + "}");
                            this.sockets.broadcast(input[1].getBytes());
                        } catch (IndexOutOfBoundsException e) {
                            System.out.println("Please provide -s tag");
                        }
                        break;
                    case ("K"):
                        System.out.println("Killing Server");
                        this.dispose();
                        break OUTER_LOOP;
                    default:
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static boolean addSyncVar(ClientInstance client, ISyncVar toSync) {
        return syncedVars.addSyncVar(client, toSync);
    }

    public void dispose() {
        try {
            this.serverRunner.cancel();
            this.serverThread.join();
            this.sockets.close();
            if (serverListener != null) {
                this.serverListener.serverClosed();
                this.serverListener = null;
            }
        } catch (InterruptedException e) {
            System.out.println(ConsoleColors.err("Error disposing"));
            System.out.println(e.toString());
        }
    }

    /**
     * ServerRunner is the thread which constantly checks for new client requests, if a client has
     * requested a socket, it will provide it a thread to communicate on and accept the socket.
     * SO_TIMEOUT is set so that sockets.acceptClient won't block the joining of the thread
     * indefinitely.
     */
    private class ServerRunner implements Runnable {
        volatile boolean open = true;

        @Override
        public void run() {
            while (open && !Thread.currentThread().isInterrupted()) {
                Socket clientSocket = sockets.acceptClient();
                if (clientSocket != null) {
                    Thread clientThread = new Thread(clientRunner(clientSocket));
                    clientThread.setDaemon(true);
                    clientThread.setName("Client " + clientSocket.getInetAddress().toString());
                    clientThread.start();
                }
            }
        }

        public void cancel() {
            this.open = false;
        }
    }

    /**
     * THe Client Runner is the thread given to each client to handle its own socket. Commands are
     * read from the input stream. It will pass all commands to the correct handler function. {@link
     * org.dragonskulle.network.ServerListener}
     *
     * @param sock
     * @return
     */
    private Runnable clientRunner(Socket sock) {
        if (sock == null) {
            return () -> {
            };
        }
        return () -> {
            try {
                boolean connected;
                String stream;
                int hasBytes = 0;
                final int MAX_TRANSMISSION_SIZE = 512;
                byte[] bArray; //max flatbuffer size
                byte[] terminateBytes = new byte[MAX_TRANSMISSION_SIZE]; //max flatbuffer size
                this.sockets.addClient(sock);
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(sock.getInputStream()));
                BufferedInputStream bIn = new BufferedInputStream(sock.getInputStream());
                PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                // create client as object
                ClientInstance client = new ClientInstance(sock.getInetAddress(), sock.getPort());
                serverListener.clientConnected(client, out);
                connected = sock.isConnected();
                while (connected) {
                    bArray = new byte[MAX_TRANSMISSION_SIZE];
                    hasBytes = bIn.read(bArray);

                    if (hasBytes != 0) {
                        if (Arrays.equals(bArray, terminateBytes)) {
                            this.sockets.terminateClient(sock); // close and remove
                            serverListener.clientDisconnected(client);
                            connected = false;
                        } else {
                            parseBytes(client, bArray);
                        }
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        };
    }


    private void parseBytes(ClientInstance client, byte[] bytes) {

        serverListener.receivedBytes(client, bytes);
//        decode bytes from flatbuffer serialisation
//        currently only one type;
        try {
            parseFlatBufferBytes(client, bytes);
        } catch (DecodingException e) {
            System.out.println(e.getMessage());
            System.out.println(new String(bytes, StandardCharsets.UTF_8));
        }

    }

    private void parseFlatBufferBytes(ClientInstance client, byte[] bytes) throws DecodingException {
        System.out.println("flatbuffer parsing");
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(bytes);
        Message message = org.dragonskulle.network.proto.Message.getRootAsMessage(buf);
        byte contentType = message.contentsType();
        if (contentType == VariableMessage.RegisterSyncVarsRequest) {
            //change parse to giving the object not bytes
            System.out.println("request is of type RegisterSyncVarsRequest");
            ArrayList<org.dragonskulle.network.proto.ISyncVar> registered = parseRegisterSyncVarsRequest((RegisterSyncVarsRequest) message.contents(new RegisterSyncVarsRequest()));
            addAllSyncVar(client, registered);
            notifyClientOfRegisteredSyncVars(client, registered);
        } else if (contentType == VariableMessage.UpdateSyncVarsRequest) {
            System.out.println("request is of type UpdateSyncVarsRequest");
            UpdateSyncVarsRequest updateRequest = (UpdateSyncVarsRequest) message.contents(new UpdateSyncVarsRequest());
            assert updateRequest != null;
            parseUpdateSyncVarsRequest(updateRequest);
        } else {
            throw new DecodingException("Message is not of valid type");
        }
    }

    private void addAllSyncVar(ClientInstance client, ArrayList<org.dragonskulle.network.proto.ISyncVar> registered) {
        for (org.dragonskulle.network.proto.ISyncVar iSyncVar : registered) {
            addSyncVar(client, FlatBufferHelpers.flatb2ISyncVar(iSyncVar));
        }
    }


//    private void addRegisteredSyncVarsToServer(ArrayList<org.dragonskulle.network.proto.ISyncVar> registered) {
//        for (org.dragonskulle.network.proto.ISyncVar iSyncVar : registered) {
//            this.synced.add(FlatBufferHelpers.flatb2ISyncVar(iSyncVar));
//        }
//    }

    private void parseUpdateSyncVarsRequest(UpdateSyncVarsRequest contents) {
        //TODO add if dormant to request and check here
        System.out.println("parsing update sync vars request");
        System.out.println("netId of update is : " + contents.netId());
        ISyncVar var = FlatBufferHelpers.flatb2ISyncVar(contents.syncVar());
        assert var != null;
        System.out.println("type of var update request is : " + var.getClass());
        System.out.println("new value of var update request is : " + var.looselyGet());
        System.out.println("id of var update request is : " + var.getId());
        grantSyncVarsRequest(var);
    }

    private void grantSyncVarsRequest(ISyncVar var) {
        if (syncedVars.updateVar(var)) {
            notifyClientsOfUpdatedVar(var);
        }
    }

    private void notifyClientsOfUpdatedVar(ISyncVar var) {
        FlatBufferBuilder builder = new FlatBufferBuilder();
        int syncVarOffset = FlatBufferHelpers.ISyncVar2flatb(builder, var);
        int response = UpdateSyncVarsResponse.createUpdateSyncVarsResponse(builder, syncVarOffset);
        int message = Message.createMessage(builder, VariableMessage.UpdateSyncVarsResponse, response);
        builder.finish(message);
        byte[] updateMessageBytes = builder.sizedByteArray();
        this.sockets.broadcast(updateMessageBytes);
        System.out.println("notified clients of sync var updation");

    }


    private void notifyClientOfRegisteredSyncVars(ClientInstance client, ArrayList<org.dragonskulle.network.proto.ISyncVar> registered) {
        FlatBufferBuilder builder = new FlatBufferBuilder();
        int[] registeredVectorOffsets = new int[registered.size()];
        int j = 0;
        for (int i = 0; i < registered.size(); i++) {
            ISyncVar var = FlatBufferHelpers.flatb2ISyncVar(registered.get(i));
            if (var != null) {
                int offset = FlatBufferHelpers.ISyncVar2flatb(builder, var);
                registeredVectorOffsets[j++] = offset;
            }
        }

        int vectorOffset = RegisteredSyncVarsResponse.createRegisteredVector(builder, registeredVectorOffsets);
        int response = RegisteredSyncVarsResponse.createRegisteredSyncVarsResponse(builder, vectorOffset);
        int message = Message.createMessage(builder, VariableMessage.RegisteredSyncVarsResponse, response);
        builder.finish(message);
        byte[] response_bytes = builder.sizedByteArray();
        this.sockets.sendBytesToClient(client, response_bytes);
        System.out.println("notified client of sync var registration");
    }


    private ArrayList<org.dragonskulle.network.proto.ISyncVar> parseRegisterSyncVarsRequest(RegisterSyncVarsRequest registerSyncVarsRequest) throws
            DecodingException {
        System.out.println("Attempting to parse request");
        boolean didRegisterSync = false;
        ArrayList<org.dragonskulle.network.proto.ISyncVar> registeredSyncVars = new ArrayList<>();
        try {
            System.out.println("is netObj dormant? " + registerSyncVarsRequest.isDormant());
            if (!registerSyncVarsRequest.isDormant()) {
                System.out.println("networkObject Id: " + registerSyncVarsRequest.netId());
                System.out.println("Contains number of sync vars: " + registerSyncVarsRequest.syncVarsLength());
                for (int i = 0; i < registerSyncVarsRequest.syncVarsLength(); i++) {
                    org.dragonskulle.network.proto.ISyncVar requestedSyncVar = registerSyncVarsRequest.syncVarsVector().get(i);
                    didRegisterSync = registerAnySyncVar(requestedSyncVar);
                    if (didRegisterSync) {
                        registeredSyncVars.add(requestedSyncVar);
                    }
                }
            }
            return registeredSyncVars;
        } catch (Exception e) {
            throw new DecodingException("Is not of RegisterSyncVarsRequest Type");
        }
    }

    private boolean registerAnySyncVar(org.dragonskulle.network.proto.ISyncVar requestedSyncVar) {
        if (requestedSyncVar.syncVarType() == AnyISyncVar.ISyncBool) {
            System.out.println("Requesting Sync of SyncBool");
            ISyncBool syncVar = (ISyncBool) requestedSyncVar.syncVar(new ISyncBool());
            assert syncVar != null;
            System.out.println("id: " + syncVar.id());
            System.out.println("data: " + syncVar.data());
        } else if (requestedSyncVar.syncVarType() == AnyISyncVar.ISyncFloat) {
            System.out.println("Requesting Sync of SyncFloat");
            ISyncFloat syncVar = (ISyncFloat) requestedSyncVar.syncVar(new ISyncFloat());
            assert syncVar != null;
            System.out.println("id: " + syncVar.id());
            System.out.println("data: " + syncVar.data());
        } else if (requestedSyncVar.syncVarType() == AnyISyncVar.ISyncLong) {
            System.out.println("Requesting Sync of SyncLong");
            ISyncLong syncVar = (ISyncLong) requestedSyncVar.syncVar(new ISyncLong());
            assert syncVar != null;
            System.out.println("id: " + syncVar.id());
            System.out.println("data: " + syncVar.data());
        } else if (requestedSyncVar.syncVarType() == AnyISyncVar.ISyncString) {
            System.out.println("Requesting Sync of SyncString");
            ISyncString syncVar = (ISyncString) requestedSyncVar.syncVar(new ISyncString());
            assert syncVar != null;
            System.out.println("id: " + syncVar.id());
            System.out.println("data: " + syncVar.data());
        } else if (requestedSyncVar.syncVarType() == AnyISyncVar.ISyncInt) {
            System.out.println("Requesting Sync of SyncInt");
            ISyncInt syncVar = (ISyncInt) requestedSyncVar.syncVar(new ISyncInt());
            assert syncVar != null;
            System.out.println("id: " + syncVar.id());
            System.out.println("data: " + syncVar.data());
        } else {
            System.out.println("Sync var is not valid");
            return false;
        }
        return true;
    }
}

