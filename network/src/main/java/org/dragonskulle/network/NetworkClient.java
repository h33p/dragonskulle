/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * This is the client usage, you will create an instance, by providing the correct server to connect
 * to. ClientListener is the handler for commands that the client receives. {@link
 * org.dragonskulle.network.ClientListener}
 */
public class NetworkClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ClientListener clientListener;
    private boolean open = true;

    public NetworkClient(String ip, int port, ClientListener listener) {
        clientListener = listener;
        try {
            socket = new Socket(ip, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
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
                open = false;
                closeAllConnections();
                clientListener.disconnected();
            }
            socket = null;
            in = null;
            out = null;
            clientListener = null;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void send(String msg) {
        if (open) out.println(msg);
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
            while (open) {
                try {
                    String s = in.readLine();
                    // if s is null we need to terminate the connections as the server has died.
                    if (s == null) {
                        //                        System.out.println("Received Null from server,
                        // server has died");
                        clientListener.disconnected();
                        this.dispose();
                        break;
                    } else { // if s is not null then we need to send this to the handler
                        // (clientListener)
                        clientListener.receivedInput(s);
                    }
                } catch (IOException ignore) { // if fails to read from in stream
                    clientListener.error("failed to read from input stream");
                    this.dispose();
                    break;
                }
            }
        };
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
    }
}
