/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.PrintWriter;

/** @author Oscar L The interface Server listener. */
public interface ServerListener {
    /**
     * Client connected event.
     *
     * @param client the client
     * @param out the out
     */
    void clientConnected(ClientInstance client, PrintWriter out);

    /**
     * Client disconnected event.
     *
     * @param client the client
     */
    void clientDisconnected(ClientInstance client);

    /**
     * Received input event.
     *
     * @param client the client
     * @param msg the msg
     */
    void receivedInput(ClientInstance client, String msg);

    /** Server closed event. */
    void serverClosed();

    /**
     * Received bytes event.
     *
     * @param client the client
     * @param bytes the bytes
     */
    void receivedBytes(ClientInstance client, byte[] bytes);
}
