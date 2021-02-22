package org.dragonskulle.network.components;

public class SyncFloat extends AbstractSync<Float> {

    public SyncFloat(String id, Float data, NetworkObject netObject) {
        super(id, data, netObject);
    }

    public SyncFloat(String id, Float data) {
        super(id, data);
    }

    public SyncFloat(Float data, NetworkObject netObject) {
        super(data, netObject);
    }

    public SyncFloat(Float data) {
        super(data);
    }
}
