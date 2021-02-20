/* (C) 2021 DragonSkulle */
package org.dragonskulle.network;

import org.dragonskulle.network.proto.*;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This is the handler for the server, it will handle most events (TODO create other events)
 */
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
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(this.trim(bytes));
        //decode bytes from flatbuffer
        //currently only one type;
        parseRegisterSyncVarsRequest(buf);
    }

    private byte[] trim(byte[] bytes)
    {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0)
        {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }

    private void parseRegisterSyncVarsRequest(java.nio.ByteBuffer buf) {
        System.out.println("Attempting to parse request");
        RegisterSyncVarsRequest registerSyncVarsRequest = RegisterSyncVarsRequest.getRootAsRegisterSyncVarsRequest(buf);
        System.out.println("is netObj dormant? " + registerSyncVarsRequest.isDormant());
        if (!registerSyncVarsRequest.isDormant()) {
            System.out.println("networkObject Id: " + registerSyncVarsRequest.netId());
            System.out.println("Contains number of sync vars: " + registerSyncVarsRequest.syncVarsLength());
            for (int i = 0; i < registerSyncVarsRequest.syncVarsLength(); i++) {
                ISyncVar requestedSyncVar = registerSyncVarsRequest.syncVarsVector().get(i);
                parseAnySyncVar(requestedSyncVar);
            }
        }
    }

    private void parseAnySyncVar(ISyncVar requestedSyncVar) {
        if (requestedSyncVar.syncVarType() == AnyISyncVar.ISyncBool) {
            System.out.println("Requesting Sync of SyncBool");
            ISyncBool syncVar = (ISyncBool) requestedSyncVar.syncVar(new ISyncBool());
            assert syncVar != null;
            System.out.println("id: " + syncVar.id());
            System.out.println("data: " + syncVar.data());
        } else if (requestedSyncVar.syncVarType() == AnyISyncVar.ISyncFloat) {
            System.out.println("Requesting Sync of SyncFloat");
            ISyncFloat syncVar = (ISyncFloat) requestedSyncVar.syncVar(new ISyncFloat());
            assert syncVar != null;
            System.out.println("id: " + syncVar.id());
            System.out.println("data: " + syncVar.data());
        } else if (requestedSyncVar.syncVarType() == AnyISyncVar.ISyncLong) {
            System.out.println("Requesting Sync of SyncLong");
            ISyncLong syncVar = (ISyncLong) requestedSyncVar.syncVar(new ISyncLong());
            assert syncVar != null;
            System.out.println("id: " + syncVar.id());
            System.out.println("data: " + syncVar.data());
        } else if (requestedSyncVar.syncVarType() == AnyISyncVar.ISyncString) {
            System.out.println("Requesting Sync of SyncString");
            ISyncString syncVar = (ISyncString) requestedSyncVar.syncVar(new ISyncString());
            assert syncVar != null;
            System.out.println("id: " + syncVar.id());
            System.out.println("data: " + syncVar.data());
        } else if (requestedSyncVar.syncVarType() == AnyISyncVar.ISyncInt) {
            System.out.println("Requesting Sync of SyncInt");
            ISyncInt syncVar = (ISyncInt) requestedSyncVar.syncVar(new ISyncInt());
            assert syncVar != null;
            System.out.println("id: " + syncVar.id());
            System.out.println("data: " + syncVar.data());
        } else {
            System.out.println("Sync var is not valid");
        }
    }
}

class LogServerAlive extends TimerTask {
    public void run() {
        System.out.println("[SE~TT] Server Alive @ " + System.currentTimeMillis());
    }
}
