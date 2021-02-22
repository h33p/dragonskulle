package org.dragonskulle.network.components;


public class SyncString extends AbstractSync<String>{

    public SyncString(String id, String data, NetworkObject netObject) {
        super(id, data, netObject);
    }

    public SyncString(String id, String data) {
        super(id, data);
    }

    public SyncString(String data, NetworkObject netObject) {
        super(data, netObject);
    }

    public SyncString(String data) {
        super(data);
    }
}
