/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

public class SyncShort extends AbstractSync<Short> {
    public SyncShort(String id, Short data) {
        super(id, data);
    }

    public SyncShort(Short data) {
        super(data);
    }
}
