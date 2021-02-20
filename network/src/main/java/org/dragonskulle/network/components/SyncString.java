package org.dragonskulle.network.components;

public class SyncString extends ISyncVar<String>{
    public SyncString(String data) {
        super(data);
    }

    public SyncString(String id,String data) {
        super(id,data);
    }

    public SyncString() {
        super(null);
    }
}
