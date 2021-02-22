package org.dragonskulle.network.components;

public class SyncBool extends AbstractSync<Boolean> {
    public SyncBool(String id, Boolean data, NetworkObject netObject) {
        super(id, data, netObject);
    }

    public SyncBool(String id, Boolean data) {
        super(id, data);
    }

    public SyncBool(Boolean data, NetworkObject netObject) {
        super(data, netObject);
    }

    public SyncBool(Boolean data) {
        super(data);
    }
};
