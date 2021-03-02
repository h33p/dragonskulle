/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;
// based on
// https://github.com/TheDudeFromCI/WraithEngine/tree/5397e2cfd75c257e4d96d0fd6414e302ab22a69c/WraithEngine/src/wraith/library/Multiplayer

import com.sun.xml.internal.org.jvnet.mimepull.DecodingException;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.dragonskulle.network.components.Capital;
import org.dragonskulle.network.components.NetworkObject;
import org.dragonskulle.network.components.Networkable;

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
    private ServerGameInstance game;
    private final ArrayList<NetworkObject> networkObjects = new ArrayList<>();

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
            this.createGame();

            serverRunner = new ServerRunner();
            serverThread = new Thread(this.serverRunner);
            serverThread.setDaemon(true);
            serverThread.setName("Server");
            System.out.println("[S] Starting server");
            System.out.println("[S] TODO Setup Game");
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

    static void executeBytes(
            byte messageType, byte[] payload, SendBytesToClientCurry sendBytesToClient) {
        byte[] message;
        switch (messageType) {
            case (byte) 22:
                message = NetworkMessage.build((byte) 20, "TOSPAWN".getBytes());
                sendBytesToClient.send(message);
                break;
            default:
                System.out.println("Should implement spawn and create building ____");
                message = NetworkMessage.build((byte) 20, "TOSPAWN".getBytes());
                sendBytesToClient.send(message);
                break;
        }
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
            System.out.println("Error disposing");
            System.out.println(e.toString());
        }
    }

    public void createGame() {
        this.game = new ServerGameInstance();
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
                if (game.isSetup()) {
                    Socket clientSocket = sockets.acceptClient();
                    if (clientSocket != null) {
                        Thread clientThread = new Thread(clientRunner(clientSocket));
                        clientThread.setDaemon(true);
                        clientThread.setName("Client " + clientSocket.getInetAddress().toString());
                        clientThread.start();
                    }
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
            return () -> {};
        }
        return () -> {
            try {
                // should spawn map and capitol here instead
                boolean connected;
                String stream;
                int hasBytes = 0;
                final int MAX_TRANSMISSION_SIZE = NetworkConfig.MAX_TRANSMISSION_SIZE;
                byte[] bArray; // max flatbuffer size
                byte[] terminateBytes = new byte[MAX_TRANSMISSION_SIZE]; // max flatbuffer size
                this.sockets.addClient(sock);
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(sock.getInputStream()));
                BufferedInputStream bIn = new BufferedInputStream(sock.getInputStream());
                PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                // create client as object
                ClientInstance client = new ClientInstance(sock.getInetAddress(), sock.getPort());
                serverListener.clientConnected(client, out);
                connected = sock.isConnected();

                if (connected) {
                    NetworkObject networkObject =
                            new NetworkObject(
                                    client,
                                    this.sockets::broadcast,
                                    this.sockets::sendBytesToClient);
                    networkObject.spawnMap(this.game.cloneMap());
                    String capitalId = networkObject.spawnCapital();
                    this.networkObjects.add(networkObject);

                    // Simulation of calling fixed update;
                    Timer timer = new Timer();
                    int begin = 0;
                    int timeInterval = 1000;
                    FixedUpdateSimulation fixedUpdate = this::fixedBroadcastUpdate;
                    timer.schedule(
                            new TimerTask() {
                                int counter = 0;

                                @Override
                                public void run() {
                                    fixedUpdate.call();
                                    counter++;
                                    if (counter >= 20) {
                                        timer.cancel();
                                    }
                                }
                            },
                            begin,
                            timeInterval);

                    Server self = this;
                    //                    set bool of capitol at some point in the future
                    timer.schedule(
                            new TimerTask() {
                                @Override
                                public void run() {
                                    Capital networkCapital =
                                            (Capital) getNetworkableChild(networkObject, capitalId);
                                    if (networkCapital != null) {
                                        networkCapital.setBooleanSyncMe(true);
                                    }
                                }
                            },
                            3000);

                    // set string of capitol at some point in the future
                    timer.schedule(
                            new TimerTask() {
                                @Override
                                public void run() {
                                    Capital networkCapital =
                                            (Capital) getNetworkableChild(networkObject, capitalId);
                                    if (networkCapital != null) {
                                        networkCapital.setStringSyncMeAlso("goodbye world");
                                    }
                                }
                            },
                            3000);
                }
                while (connected) {
                    bArray = NetworkMessage.readMessageFromStream(bIn);
                    if (bArray.length != 0) {
                        if (Arrays.equals(bArray, terminateBytes)) {
                            this.sockets.terminateClient(sock); // close and remove
                            serverListener.clientDisconnected(client);
                            connected = false;
                        } else {
                            processBytes(client, bArray);
                        }
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        };
    }

    private Networkable getNetworkableChild(NetworkObject networkObject, String id) {
        final NetworkObject serverNetworkObject =
                this.networkObjects.get(this.networkObjects.indexOf(networkObject));
        Networkable child = serverNetworkObject.get(id);
        return child;
    }

    private void processBytes(ClientInstance client, byte[] bytes) {

        serverListener.receivedBytes(client, bytes);
        try {
            parseBytes(client, bytes);
        } catch (DecodingException e) {
            System.out.println(e.getMessage());
            System.out.println(new String(bytes, StandardCharsets.UTF_8));
        }
    }

    private void parseBytes(ClientInstance client, byte[] bytes) throws DecodingException {
        System.out.println("bytes parsing");
        try {
            NetworkMessage.parse(
                    bytes, (parsedBytes) -> this.sockets.sendBytesToClient(client, parsedBytes));
        } catch (Exception e) {
            System.out.println("Error in parseBytes");
            e.printStackTrace();
            throw new DecodingException("Message is not of valid type");
        }
    }

    public interface SendBytesToClientCurry {
        void send(byte[] bytes);
    }

    private interface FixedUpdateSimulation {
        void call();
    }

    public void fixedBroadcastUpdate() {
        System.out.println("fixed broadcast update");
        for (NetworkObject networkObject : this.networkObjects) {
            networkObject.broadcastUpdate();
        }
    }
}
