/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * @author Oscar L ClientEars is the client implementation of ClientListener, this is where custom
 *     executions can be defined for different commands received. It is mainly used just to log
 *     events to the console.
 */
public class ClientEars implements ClientListener {
    private static final Logger mLogger = Logger.getLogger(ClientEars.class.getName());

    @Override
    public void unknownHost() {
        mLogger.fine("[Client] Unknown Host");
    }

    @Override
    public void couldNotConnect() {
        mLogger.fine("[Client] Could not connect");
    }

    @Override
    public void receivedInput(String msg) {
        mLogger.fine("[Client] Received Input {" + msg + "}");
    }

    @Override
    public void receivedBytes(byte[] bytes) {
        if (bytes.length > 15) {
            mLogger.fine(
                    "[Client] Received Bytes :: "
                            + Arrays.toString(Arrays.copyOfRange(bytes, 0, 15)));
            mLogger.fine(
                    " \t...\t   "
                            + Arrays.toString(
                                    Arrays.copyOfRange(bytes, bytes.length - 15, bytes.length)));
        }
    }

    @Override
    public void serverClosed() {
        mLogger.fine("[Client] Server Closed");
    }

    @Override
    public void disconnected() {
        mLogger.fine("[Client] Disconnected from server");
    }

    @Override
    public void connectedToServer() {
        mLogger.fine("[Client] Connected from server");
    }

    @Override
    public void error(String s) {
        mLogger.fine("[Client] ERROR: " + s);
    }
}
