/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.PrintWriter;

public interface ServerListener {
    void clientConnected(ClientInstance client, PrintWriter out);

    void clientDisconnected(ClientInstance client);

    void receivedInput(ClientInstance client, String msg);

    void serverClosed();

    void receivedBytes(ClientInstance client, byte[] bytes);
}
