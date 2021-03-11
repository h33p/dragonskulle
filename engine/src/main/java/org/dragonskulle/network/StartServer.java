/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.dragonskulle.core.Scene;

/** @author Oscar L How to start the server Start server. */
public class StartServer {
    private static final Logger mLogger = Logger.getLogger(StartServer.class.getName());

    /** The Server. */
    public Server server;
    /** The Port. */
    static final int PORT = 7000;

    public StartServer(AtomicInteger networkObjectCounter) {
        attachShutDownHook();
        /** The Server listener. */
        ServerListener serverListener = new ServerEars();
        server = new Server(PORT, serverListener, networkObjectCounter);
    }

    public StartServer(
            AtomicInteger networkObjectCounter,
            boolean autoProcessMessages,
            boolean startFixedUpdate) {
        attachShutDownHook();
        /** The Server listener. */
        ServerListener serverListener = new ServerEars();
        if (autoProcessMessages) {
            server = new Server(PORT, serverListener, autoProcessMessages, networkObjectCounter);
            if (startFixedUpdate) {
                server.startFixedUpdate();
            }
        }
    }

    public static void main(String[] args) {
        AtomicInteger networkObjectCounter = new AtomicInteger(0);
        StartServer ss = new StartServer(networkObjectCounter);
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

    public void linkToScene(Scene mainScene) {
        this.server.linkToScene(mainScene);
    }
}
