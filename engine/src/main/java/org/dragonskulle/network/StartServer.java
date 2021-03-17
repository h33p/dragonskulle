/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.dragonskulle.core.Scene;
import org.dragonskulle.network.components.ServerNetworkManager;

/** @author Oscar L How to start the server Start server. */
public class StartServer {
    private static final Logger mLogger = Logger.getLogger(StartServer.class.getName());

    /** The Server. */
    public Server mServer;
    /** The Port. */
    static final int PORT = 7000;

    public StartServer(AtomicInteger networkObjectCounter) {
        attachShutDownHook();
        /** The Server listener. */
        ServerListener serverListener = new ServerEars();
        mServer = new Server(PORT, serverListener, networkObjectCounter);
    }

    public StartServer(
            AtomicInteger networkObjectCounter,
            boolean autoProcessMessages,
            boolean startFixedUpdate) {
        attachShutDownHook();
        /** The Server listener. */
        ServerListener serverListener = new ServerEars();
        if (autoProcessMessages) {
            mServer = new Server(PORT, serverListener, autoProcessMessages, networkObjectCounter);
            if (startFixedUpdate) {
                mServer.startFixedUpdateDetachedFromGame();
            }
        }
    }

    public StartServer(
            AtomicInteger networkObjectCounter,
            boolean autoProcessMessages,
            boolean startFixedUpdate,
            Scene mainScene) {
        attachShutDownHook();
        /** The Server listener. */
        ServerListener serverListener = new ServerEars();
        if (autoProcessMessages) {
            mServer =
                    new Server(
                            PORT,
                            serverListener,
                            autoProcessMessages,
                            networkObjectCounter,
                            mainScene);
            if (startFixedUpdate) {
                mServer.startFixedUpdateDetachedFromGame();
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
                                    mLogger.fine("Shutting down server");
                                    this.mServer.dispose();
                                }));

        mLogger.fine("Shut Down Hook Attached.");
    }

    /** Disposes of the server instance. */
    public void dispose() {
        mServer.dispose();
    }

    /** Clears pending requests on the server instance. */
    public void clearPendingRequests() {
        mServer.clearPendingRequests();
    }

    /**
     * Links scene to server instance.
     *
     * @param scene the scene
     */
    public void linkToScene(Scene scene) {
        this.mServer.linkToScene(scene);
    }

    public ServerNetworkManager createNetworkManager() {
        return new ServerNetworkManager(this.mServer::fixedBroadcastUpdate, this::dispose);
    }
}
