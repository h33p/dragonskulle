package org.dragonskulle.network;

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
}
