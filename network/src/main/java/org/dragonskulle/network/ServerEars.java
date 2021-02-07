package org.dragonskulle.network;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class ServerEars implements ServerListener {
    final Queue<String> log;
    final Timer alive_timer;

    ServerEars() {
        System.out.println("Creating ServerLIstener");
        log = new LinkedList<>();
        System.out.println("Is log defined? " + log.toString());
        alive_timer = new Timer();
        alive_timer.schedule(new LogServerAlive(), 0, 5000);
    }

    @Override
    public void clientConnected(ClientInstance client, PrintWriter out) {
        System.out.println("Connecting client in ServerEars");
        System.out.println("loggable? " + (log.toString()));
        log.add("[SE] Client Connected");
    }

    @Override
    public void clientDisconnected(ClientInstance client) {
        log.add("[SE] Client Disconnected");

    }

    @Override
    public void receivedInput(ClientInstance client, String msg) {
        log.add("[SE] Received Input From Client");

    }

    @Override
    public void serverClosed() {
        log.add("[SE] Server Closed");
    }

    @Override
    public void viewLog() {
        if (!this.log.isEmpty()) {
            System.out.println("[SE] " + log.poll());
        }
    }
}

class LogServerAlive extends TimerTask {
    public void run() {
        System.out.println("[SE] Server Alive @ " + System.currentTimeMillis());
    }
}
