package org.dragonskulle.network.components;


public class SyncInt extends AbstractSync<Integer> {

    public SyncInt(String id, Integer data, NetworkObject netObject) {
        super(id, data, netObject);
    }

    public SyncInt(String id, Integer data) {
        super(id, data);
    }

    public SyncInt(Integer data, NetworkObject netObject) {
        super(data, netObject);
    }

    public SyncInt(Integer data) {
        super(data);
    }
}
