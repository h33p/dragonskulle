/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import org.dragonskulle.network.components.sync.SyncBool;
import org.dragonskulle.network.components.sync.SyncString;


public class Capital extends Networkable {
    SyncBool syncMe = new SyncBool(false);
//    SyncString syncMeAlso = new SyncString("Hello World");

    public Capital() {
        super();
    }

    public void setBooleanSyncMe(boolean val) {
        this.syncMe.set(val);
    }

//    public void setStringSyncMeAlso(String val) {
//        this.syncMeAlso.set(val);
//    }

}
