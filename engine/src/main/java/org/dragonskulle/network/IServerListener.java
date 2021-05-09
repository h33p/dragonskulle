/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.DataInput;
import java.io.IOException;

/**
 * The interface Server listener.
 *
 * @author Aurimas Blažulionis
 * @author Oscar L
 */
public interface IServerListener {
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
     * Client loaded event.
     *
     * <p>This is called after the client was added to the client list on the main thread.
     *
     * @param client the client
     */
    void clientFullyConnected(ServerClient client);

    /**
     * Client loaded event.
     *
     * <p>This is called after the host has started game when the client has fully loaded. Networked
     * objects can only be spawned after this point
     *
     * @param client the client
     */
    void clientLoaded(ServerClient client);

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
    void clientComponentRequest(ServerClient client, int objectID, int requestID, DataInput stream)
            throws IOException;
}
