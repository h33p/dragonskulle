/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

/**
 * @author Oscar L
 *     <p>This interface is to handle client commands, this can be extended to run command when a
 *     message is received. Important events are logged here
 */
public interface IClientListener {
    /** Unknown host. */
    void unknownHost();

    /** Could not connect. */
    void couldNotConnect();

    /** Server closed. */
    void serverClosed();

    /** Disconnected. */
    void disconnected();

    /**
     * Connected to server.
     *
     * @param netID network ID the server gave us
     */
    void connectedToServer(int netID);

    /**
     * Error.
     *
     * @param s the s
     */
    void error(String s);

    /**
     * Update networkable from bytes, this is authored by the server.
     *
     * @param payload the payload
     */
    void updateNetworkObject(byte[] payload);

    /**
     * Spawn a network object from bytes, this is authored by the server.
     *
     * @param payload payload containing the object info
     */
    void spawnNetworkObject(byte[] payload);

    /**
     * Update the server's state on the client.
     *
     * @param payload payload containing the server's world state
     */
    void updateServerState(byte[] payload);
}
