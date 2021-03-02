/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

public class SyncLong extends AbstractSync<Long> {
    public SyncLong(String id, Long data) {
        super(id, data);
    }

    public SyncLong(Long data) {
        super(data);
    }
}
