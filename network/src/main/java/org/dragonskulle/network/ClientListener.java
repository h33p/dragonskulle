/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

/**
 * This interface is to handle client commands, this can be extended to run command when a message is received. Important events are logged here
 */
public interface ClientListener {
    /**
     * Unknown host.
     */
    void unknownHost();

    /**
     * Could not connect.
     */
    void couldNotConnect();

    /**
     * Received input.
     *
     * @param msg the msg
     */
    void receivedInput(String msg);

    /**
     * Received bytes.
     *
     * @param bytes the bytes
     */
    void receivedBytes(byte[] bytes);

    /**
     * Server closed.
     */
    void serverClosed();

    /**
     * Disconnected.
     */
    void disconnected();

    /**
     * Connected to server.
     */
    void connectedToServer();

    /**
     * Error.
     *
     * @param s the s
     */
    void error(String s);
}
