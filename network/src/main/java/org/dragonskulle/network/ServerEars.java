/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/** This is the handler for the server, it will handle most events (TODO create other events) */
public class ServerEars implements ServerListener {
    ListenableQueue<String> log;
    Timer aliveTimer;

    ServerEars() {
        System.out.println("Creating ServerListener");
        log = new ListenableQueue<>(new LinkedList<>());
        log.registerListener((e) -> System.out.println("[SE-LOG] " + log.poll()));
        aliveTimer = new Timer();
        aliveTimer.schedule(new LogServerAlive(), 0, 15000);
    }

    @Override
    public void clientConnected(ClientInstance client, PrintWriter out) {
        log.add("Client Connected");
    }

    @Override
    public void clientDisconnected(ClientInstance client) {
        log.add("Client Disconnected");
    }

    @Override
    public void receivedInput(ClientInstance client, String msg) {

        log.add("Received Input From Client: " + msg);
    }

    @Override
    public void serverClosed() {
        log.add("Server Closed");
        this.aliveTimer.cancel();
    }

    @Override
    public void receivedBytes(ClientInstance client, byte[] bytes) {
        System.out.println("--\ngot bytes");
        System.out.println(Arrays.toString(bytes));
    }
}

class LogServerAlive extends TimerTask {
    public void run() {
        System.out.println("[SE~TT] Server Alive @ " + System.currentTimeMillis());
    }
}
