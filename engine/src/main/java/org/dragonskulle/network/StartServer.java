/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.util.logging.Logger;

/** @author Oscar L How to start the server Start server. */
public class StartServer {
    private static final Logger mLogger = Logger.getLogger(StartServer.class.getName());

    /** The Server. */
    public Server server;
    /** The Port. */
    static final int PORT = 7000;

    public StartServer() {
        attachShutDownHook();
        /** The Server listener. */
        ServerListener serverListener = new ServerEars();
        server = new Server(PORT, serverListener);
    }

    public StartServer(boolean autoProcessMessages, boolean startFixedUpdate) {
        attachShutDownHook();
        /** The Server listener. */
        ServerListener serverListener = new ServerEars();
        if (autoProcessMessages) {
            server = new Server(PORT, serverListener, autoProcessMessages);
            if (startFixedUpdate) {
                server.startFixedUpdate();
            }
        }
    }

    public static void main(String[] args) {
        StartServer ss = new StartServer();
    }

    /** Attach shut down hook. */
    public void attachShutDownHook() {
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    mLogger.info("Shutting down server");
                                    this.server.dispose();
                                }));

        mLogger.info("Shut Down Hook Attached.");
    }

    public void dispose() {
        server.dispose();
    }

    public void clearPendingRequests() {
        server.clearPendingRequests();
    }
}
