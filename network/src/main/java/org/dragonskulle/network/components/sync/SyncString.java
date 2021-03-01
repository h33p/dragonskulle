/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

public class SyncString extends AbstractSync<String> {
    public SyncString(String id, String data) {
        super(id, data);
    }

    public SyncString(String data) {
        super(data);
    }

};
