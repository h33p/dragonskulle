/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

/** How to start the server Start server. */
public class StartServer {
    /** The Server listener. */
    static ServerListener serverListener;
    /** The Server. */
    public static Server server;
    /** The Port. */
    static final int PORT = 7000;

    StartServer(){
        attachShutDownHook();
        serverListener = new ServerEars();
        server = new Server(PORT, serverListener);
    }

//    public static void main(String[] args) {
//        attachShutDownHook();
//        serverListener = new ServerEars();
//        server = new Server(PORT, serverListener);
//    }

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

    public void dispose(){
        server.dispose();
    }
}
