/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import org.dragonskulle.network.components.sync.SyncBool;

public class Capitol extends Networkable {
    SyncBool syncMe = new SyncBool(false);

    public Capitol() {
        super();
        System.out.println("--starting to change syncvar values--");
    }

    public void setBooleanSyncMe(boolean val) {
        this.syncMe.set(val);
    }
}
