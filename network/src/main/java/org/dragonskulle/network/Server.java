package org.dragonskulle.network;
//based on https://github.com/TheDudeFromCI/WraithEngine/tree/5397e2cfd75c257e4d96d0fd6414e302ab22a69c/WraithEngine/src/wraith/library/Multiplayer

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private int port;
    private boolean open = true;
    private ServerListener serverListener;
    private final SocketStore sockets = new SocketStore();
    private Thread serverThread;

    public Server(int port, ServerListener listener) {
        System.out.println("[S] Setting up server");
        serverListener = listener;
        try {
            ServerSocket server_sock = new ServerSocket(port, 0, InetAddress.getByName(null)); //sets up on localhost
            sockets.initServer(server_sock);
            if (this.port == 0) {
                this.port = sockets.getServerPort();
            } else {
                this.port = port;
            }

            serverThread = new Thread(this.server_runner());
            serverThread.setDaemon(true);
            serverThread.setName("Server");
            System.out.println("[S] Starting server");
            serverThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void dispose() {
        open = false;
        this.sockets.close();
        serverListener.serverClosed();
        serverListener = null;
    }

    private Runnable server_runner() {
        return () -> {
            while (open) {
                final Socket client_socket = this.sockets.acceptClient();
                Thread clientThread = new Thread(this.client_runner(client_socket));
                clientThread.setDaemon(true);
                clientThread.setName("Client " + client_socket.getInetAddress().toString());
                clientThread.start();
            }
        };
    }

    private Runnable client_runner(Socket sock) {
        return () -> {
            try {
                boolean connected = false;
                String stream = "";
                this.sockets.addClient(sock);
                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                //create client as object
                ClientInstance client = new ClientInstance(sock.getInetAddress(), sock.getPort());
                serverListener.clientConnected(client, out);
                connected = sock.isConnected();
                while (connected) {
                    try {
                        stream = in.readLine();
                        if(stream==null){
                            throw new IOException();
                        }
                        serverListener.receivedInput(client, stream);

                    } catch (IOException e) {
                        //if client disconnected, remove it
                        try {
                            connected = this.sockets.terminateClient(sock); //close and remove
                            serverListener.clientDisconnected(client);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                        break;
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        };
    }
}