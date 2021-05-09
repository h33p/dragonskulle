/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.DataInput;
import java.io.IOException;

/**
 * The interface Client listener.
 *
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

    /** Host started the game. */
    void hostStartedGame();

    /**
     * Error.
     *
     * @param s the s
     */
    void error(String s);

    /**
     * Update networkable from bytes, this is authored by the server.
     *
     * @param stream stream containing the message
     * @throws IOException if there was an error parsing the message
     */
    void updateNetworkObject(DataInput stream) throws IOException;

    /**
     * Spawn a network object from bytes, this is authored by the server.
     *
     * @param stream stream containing the message
     * @throws IOException if there was an error parsing the message
     */
    void spawnNetworkObject(DataInput stream) throws IOException;

    /**
     * Update the server's state on the client.
     *
     * @param stream stream containing the message
     * @throws IOException if there was an error parsing the message
     */
    void updateServerState(DataInput stream) throws IOException;

    /**
     * Invoke a server event on the client's object.
     *
     * @param stream stream containing the message
     * @throws IOException if there was an error parsing the message
     */
    void objectEvent(DataInput stream) throws IOException;
}
