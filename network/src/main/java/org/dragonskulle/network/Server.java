/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;
// based on
// https://github.com/TheDudeFromCI/WraithEngine/tree/5397e2cfd75c257e4d96d0fd6414e302ab22a69c/WraithEngine/src/wraith/library/Multiplayer

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server {
    private int port;
    private ServerListener serverListener;
    private final SocketStore sockets = new SocketStore();
    private Thread serverThread;
    private ServerRunner serverRunner;

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
                            this.sockets.broadcast(input[1]);
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

    private Runnable clientRunner(Socket sock) {
        if (sock == null) {
            return () -> {};
        }
        return () -> {
            try {
                boolean connected;
                String stream;
                this.sockets.addClient(sock);
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(sock.getInputStream()));
                PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                // create client as object
                ClientInstance client = new ClientInstance(sock.getInetAddress(), sock.getPort());
                serverListener.clientConnected(client, out);
                connected = sock.isConnected();
                while (connected) {
                    try {
                        stream = in.readLine();
                        if (stream == null) {
                            throw new IOException();
                        }
                        serverListener.receivedInput(client, stream);

                    } catch (IOException e) {
                        // if client disconnected, remove it
                        try {
                            this.sockets.terminateClient(sock); // close and remove
                            serverListener.clientDisconnected(client);
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                        connected = false;
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        };
    }
}
