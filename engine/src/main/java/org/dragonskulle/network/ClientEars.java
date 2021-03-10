/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * @author Oscar L ClientEars is the client implementation of ClientListener, this is where custom
 *     executions can be defined for different commands received.
 */
public class ClientEars implements ClientListener {
    private static final Logger mLogger = Logger.getLogger(ClientEars.class.getName());

    @Override
    public void unknownHost() {
        mLogger.info("[Client] Unknown Host");
    }

    @Override
    public void couldNotConnect() {
        mLogger.info("[Client] Could not connect");
    }

    @Override
    public void receivedInput(String msg) {
        mLogger.info("[Client] Received Input {" + msg + "}");
    }

    @Override
    public void receivedBytes(byte[] bytes) {
        if (bytes.length > 15) {
            mLogger.info(
                    "[Client] Received Bytes :: "
                            + Arrays.toString(Arrays.copyOfRange(bytes, 0, 15)));
            mLogger.info(
                    " \t...\t   "
                            + Arrays.toString(
                                    Arrays.copyOfRange(bytes, bytes.length - 15, bytes.length)));
        }
    }

    @Override
    public void serverClosed() {
        mLogger.info("[Client] Server Closed");
    }

    @Override
    public void disconnected() {
        mLogger.info("[Client] Disconnected from server");
    }

    @Override
    public void connectedToServer() {
        mLogger.info("[Client] Connected from server");
    }

    @Override
    public void error(String s) {
        mLogger.info("[Client] ERROR: " + s);
    }
}
