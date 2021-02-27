/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

public class SyncBool extends AbstractSync<Boolean> {
    public SyncBool(String id, Boolean data) {
        super(id, data);
    }

    public SyncBool(Boolean data) {
        super(data);
    }

};
