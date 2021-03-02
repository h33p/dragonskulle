/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import org.dragonskulle.network.components.sync.SyncBool;
import org.dragonskulle.network.components.sync.SyncString;

/**
 * The Capital Component.
 */
public class Capital extends Networkable {
    /**
     * A syncable field.
     */
    SyncBool syncMe = new SyncBool(false);
    /**
     * A syncable field.
     */
    SyncString syncMeAlso = new SyncString("Hello World");

    /**
     * Instantiates a new Capital.
     */
    public Capital() {
        super();
    }

    /**
     * Modifies the boolean sync me.
     *
     * @param val the val
     */
    public void setBooleanSyncMe(boolean val) {
        this.syncMe.set(val);
    }

    /**
     * Modifies the string sync me also.
     *
     * @param val the val
     */
    public void setStringSyncMeAlso(String val) {
        this.syncMeAlso.set(val);
    }
}
