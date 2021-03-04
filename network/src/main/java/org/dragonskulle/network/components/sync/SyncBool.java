/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

/** @author Oscar L The type Sync bool. */
public class SyncBool extends AbstractSync<Boolean> {
    /**
     * Instantiates a new Sync bool.
     *
     * @param id the id
     * @param data the data
     */
    public SyncBool(String id, Boolean data) {
        super(id, data);
    }

    /**
     * Instantiates a new Sync bool.
     *
     * @param data the data
     */
    public SyncBool(Boolean data) {
        super(data);
    }
};
