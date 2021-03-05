/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

/** @author Oscar L How to start the server Start server. */
public class StartServer {
    /** The Server. */
    public Server server;
    /** The Port. */
    static final int PORT = 7000;

    StartServer() {
        attachShutDownHook();
        /** The Server listener. */
        ServerListener serverListener = new ServerEars();
        server = new Server(PORT, serverListener);
    }

    StartServer(boolean debug) {
        attachShutDownHook();
        /** The Server listener. */
        ServerListener serverListener = new ServerEars();
        if(debug) {
            server = new Server(PORT, serverListener, debug);
        }
    }

    public static void main(String[] args) {
        StartServer ss = new StartServer(true);
    }

    /** Attach shut down hook. */
    public void attachShutDownHook() {
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    System.out.println("Shutting down server");
                                    this.server.dispose();
                                }));

        System.out.println("Shut Down Hook Attached.");
    }

    public void dispose() {
        server.dispose();
    }
}
