package org.dragonskulle.network;

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
                this.sockets.addClient(sock);
                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                //create client as object
                System.out.println("Attempting to create client");
                ClientInstance client = new ClientInstance(sock.getInetAddress(), sock.getPort());
                System.out.println("Created client");
                System.out.println(client.toString());
//something funky happening here
                serverListener.clientConnected(client, out);

                System.out.println("Connected client");

                while (open) {
                    try {
                        serverListener.receivedInput(client, in.readLine());
                    } catch (IOException e) {
                        //if client disconnected, remove it
                        serverListener.clientDisconnected(client);
                        try {
                            if (!sock.isClosed()) {
                                sock.shutdownOutput();
                                sock.close();
                            }
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                        this.sockets.removeClient(sock);
                        return;
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            this.sockets.terminateClient(sock); //close and remove
        };
    }
}