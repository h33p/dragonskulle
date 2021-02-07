package org.dragonskulle.network;


public class StartServer {
    static ServerListener serverListener;
    static Server server;
    final static int port = 7000;

    StartServer() {
        serverListener = new ServerEars();
    }

    public static void main(String[] args) {
        attachShutDownHook();
        server = new Server(port, serverListener);
        StartServer runner = new StartServer();


        while (true) {
            serverListener.viewLog();
        }
    }

    public static void attachShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Shutting down server");
                server.dispose();
            }
        });

        System.out.println("Shut Down Hook Attached.");
    }

}