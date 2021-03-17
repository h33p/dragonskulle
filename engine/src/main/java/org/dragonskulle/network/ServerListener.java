/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * The interface Server listener.
 *
 * @author Aurimas Blažulionis
 * @author Oscar L
 */
public interface ServerListener {
    /**
     * Client connected event.
     *
     * <p>This will be called from the client socket thread.
     *
     * @param client the client
     * @return which network ID was allocated for the client
     */
    int clientConnected(ServerClient client);

    /**
     * Client activated event.
     *
     * <p>This will be called from the main thread when the client gets fully connected.
     *
     * @param client the client
     */
    void clientActivated(ServerClient client);

    /**
     * Client disconnected event.
     *
     * <p>This will be called from the client socket thread.
     *
     * @param client the client
     */
    void clientDisconnected(ServerClient client);

    /**
     * Client component request.
     *
     * <p>This will be called from the main thread.
     *
     * @param client requesting client
     * @param objectID target network object ID
     * @param requestID request ID on the object
     * @param stream stream of the request
     */
    void clientComponentRequest(
            ServerClient client, int objectID, int requestID, DataInputStream stream)
            throws IOException;
}
