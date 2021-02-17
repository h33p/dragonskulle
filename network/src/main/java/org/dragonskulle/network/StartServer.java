/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

public class StartServer {
    static ServerListener serverListener;
    static Server server;
    static final int PORT = 7000;

    StartServer() {}

    public static void main(String[] args) {
        attachShutDownHook();
        serverListener = new ServerEars();
        server = new Server(PORT, serverListener);
    }

    public static void attachShutDownHook() {
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread() {
                            @Override
                            public void run() {
                                System.out.println("Shutting down server");
                                server.dispose();
                            }
                        });

        System.out.println("Shut Down Hook Attached.");
    }
}
