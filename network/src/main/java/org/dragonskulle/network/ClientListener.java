/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;
/** This interface is to handle client commands. */
public interface ClientListener {
    void unknownHost();

    void couldNotConnect();

    void receivedInput(String msg);

    void serverClosed();

    void disconnected();

    void connectedToServer();

    void error(String s);
}
