/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

/** @author Oscar L How to start the server Start server. */
public class StartServer {
    /** The Server. */
    public static Server server;
    /** The Port. */
    static final int PORT = 7000;

    StartServer() {
        attachShutDownHook();
        /** The Server listener. */
        ServerListener serverListener = new ServerEars();
        server = new Server(PORT, serverListener);
    }

    public static void main(String[] args) {
        StartServer ss = new StartServer();
    }

    /** Attach shut down hook. */
    public static void attachShutDownHook() {
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    System.out.println("Shutting down server");
                                    server.dispose();
                                }));

        System.out.println("Shut Down Hook Attached.");
    }

    public void dispose() {
        server.dispose();
    }
}
