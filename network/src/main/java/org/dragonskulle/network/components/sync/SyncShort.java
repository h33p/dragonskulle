/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

/** The type Sync short. */
public class SyncShort extends AbstractSync<Short> {
    /**
     * Instantiates a new Sync short.
     *
     * @param id the id
     * @param data the data
     */
    public SyncShort(String id, Short data) {
        super(id, data);
    }

    /**
     * Instantiates a new Sync short.
     *
     * @param data the data
     */
    public SyncShort(Short data) {
        super(data);
    }
}
