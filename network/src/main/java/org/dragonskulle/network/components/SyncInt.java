package org.dragonskulle.network.components;

public class SyncInt extends ISyncVar<Integer>{
    public SyncInt(Integer data) {
        super(data);
    }

    public SyncInt(String id, Integer data) {
        super(id, data);
    }

    public SyncInt() {
        super(null);
    }
}
