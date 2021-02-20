package org.dragonskulle.network.components;

public class SyncFloat extends ISyncVar<Float> {

    public SyncFloat(Float data) {
        super(data);
    }

    public SyncFloat(String id, Float data) {
        super(id, data);
    }

    public SyncFloat() {
        super(null);
    }
}
