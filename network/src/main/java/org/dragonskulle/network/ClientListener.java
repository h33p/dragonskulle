/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

public interface ClientListener {
    void unknownHost();

    void couldNotConnect();

    void receivedInput(String msg);

    void serverClosed();

    void disconnected();

    void connectedToServer();
}
