/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.nio.charset.StandardCharsets;

/**
 * ClientEars is the client implementation of ClientListener, this is where custom executions can be
 * defined for different commands received.
 */
public class ClientEars implements ClientListener {
    @Override
    public void unknownHost() {
        System.out.println("[Client] Unknown Host");
    }

    @Override
    public void couldNotConnect() {
        System.out.println("[Client] Could not connect");
    }

    @Override
    public void receivedInput(String msg) {
        System.out.println("[Client] Received Input {" + msg + "}");
    }

    @Override
    public void receivedBytes(byte[] bytes) {
        System.out.println("[Client] Received Bytes " + new String(bytes, StandardCharsets.UTF_8));
    }

    @Override
    public void serverClosed() {
        System.out.println("[Client] Server Closed");
    }

    @Override
    public void disconnected() {
        System.out.println("[Client] Disconnected from server");
    }

    @Override
    public void connectedToServer() {
        System.out.println("[Client] Connected from server");
    }

    @Override
    public void error(String s) {
        System.out.println("[Client] ERROR: " + s);
    }
}
