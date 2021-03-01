/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import com.sun.xml.internal.org.jvnet.mimepull.DecodingException;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.dragonskulle.game.map.HexagonTile;
import org.dragonskulle.network.components.Capitol;

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
    private ClientGameInstance game;


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
            this.game = new ClientGameInstance();
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

    public void executeBytes(byte messageType, byte[] payload) {
        switch (messageType) {
            case (byte) 10:
                System.out.println("Should update requested object");
                updateNetworkable(payload);
                break;
            case (byte) 20:
                try {
                    System.out.println("Trying to spawn map");
                    HexagonTile[][] map = deserializeMap(payload);
                    this.game.spawnMap(map);
                    System.out.println("Spawned map");
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case (byte) 21:
                try {
                    System.out.println("Trying to spawn capitol");
                    Capitol capitol = deserializeCapitol(payload);
                    System.out.println("deserialized capitol bytes, now spawning locally");
                    this.game.spawnCapitol(capitol);
                    System.out.println("Spawned capitol");
                } catch (DecodingException e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.out.println(
                        "unsure of what to do with message as unknown type byte " + messageType);
                break;
        }
    }

    private Capitol deserializeCapitol(byte[] payload) throws DecodingException {
        Capitol capitol = new Capitol().from(payload);
        return capitol;
    }

    private HexagonTile[][] deserializeMap(byte[] payload) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(payload);
        ObjectInput in = new ObjectInputStream(bis);
        HexagonTile[][] map = (HexagonTile[][]) in.readObject();
        return map;
    }

    private static void updateNetworkable(byte[] payload) {
        System.out.println("should implemenet updating networkable");
        System.out.println(new String(payload, Charset.defaultCharset()));
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
     * @return The Client Thread.
     */
    private Runnable clientRunner() {

        return () -> {
            byte[] bArray;
            byte[] terminateBytes = new byte[MAX_TRANSMISSION_SIZE]; // max flatbuffer size
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
            NetworkMessage.parse(bytes, this);
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
