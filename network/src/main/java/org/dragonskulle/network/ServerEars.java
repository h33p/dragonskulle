/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.PrintWriter;
import java.util.*;

public class ServerEars implements ServerListener {
    final ListenableQueue<String> log;
    final Timer alive_timer;

    ServerEars() {
        System.out.println("Creating ServerListener");
        log = new ListenableQueue<>(new LinkedList<>());
        log.registerListener((e) -> System.out.println("[SE-LOG] " + log.poll()));
        alive_timer = new Timer();
        alive_timer.schedule(new LogServerAlive(), 0, 15000);
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
        this.alive_timer.cancel();
    }
}

class LogServerAlive extends TimerTask {
    public void run() {
        System.out.println("[SE~TT] Server Alive @ " + System.currentTimeMillis());
    }
}
