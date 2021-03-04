/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.util.Arrays;

/**
 * @author Oscar L ClientEars is the client implementation of ClientListener, this is where custom
 *     executions can be defined for different commands received.
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
        if (bytes.length > 15) {
            System.out.print(
                    "[Client] Received Bytes :: "
                            + Arrays.toString(Arrays.copyOfRange(bytes, 0, 15)));
            System.out.println(
                    " \t...\t   "
                            + Arrays.toString(
                                    Arrays.copyOfRange(bytes, bytes.length - 15, bytes.length)));
        }
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
