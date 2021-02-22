package org.dragonskulle.network.components;


public class SyncLong extends AbstractSync<Long> {
    public SyncLong(String id, Long data, NetworkObject netObject) {
        super(id, data, netObject);
    }

    public SyncLong(String id, Long data) {
        super(id, data);
    }

    public SyncLong(Long data, NetworkObject netObject) {
        super(data, netObject);
    }

    public SyncLong(Long data) {
        super(data);
    }
}
