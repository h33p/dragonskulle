/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/** @author Oscar L This is the handler for the server, it will handle events */
public class ServerEars implements ServerListener {
    /** The Log of messages to be displayed. */
    private final ListenableQueue<String> mLog;
    /** The Alive ping timer. */
    private final Timer mAliveTimer;

    /** Instantiates a new Server listener. */
    ServerEars() {
        System.out.println("Creating ServerListener");
        mLog = new ListenableQueue<>(new LinkedList<>());
        mLog.registerListener((e) -> System.out.println("[SE-LOG] " + mLog.poll()));
        mAliveTimer = new Timer();
        mAliveTimer.schedule(new LogServerAlive(), 0, 15000);
    }

    @Override
    public void clientConnected(ClientInstance client, PrintWriter out) {
        mLog.add("Client Connected");
    }

    @Override
    public void clientDisconnected(ClientInstance client) {
        mLog.add("Client Disconnected");
    }

    @Override
    public void receivedInput(ClientInstance client, String msg) {

        mLog.add("Received Input From Client: " + msg);
    }

    @Override
    public void serverClosed() {
        mLog.add("Server Closed");
        this.mAliveTimer.cancel();
    }

    @Override
    public void receivedBytes(ClientInstance client, byte[] bytes) {
        System.out.println("--\ngot bytes");
        System.out.println(Arrays.toString(bytes));
    }
}

/** The type Log server alive. */
class LogServerAlive extends TimerTask {
    public void run() {
        System.out.println("[SE~TT] Server Alive @ " + System.currentTimeMillis());
    }
}