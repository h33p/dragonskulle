/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import org.dragonskulle.network.components.sync.SyncBool;
import org.dragonskulle.network.components.sync.SyncString;

/** @author Oscar L The Capital Component. */
public class Capital extends NetworkableComponent {
    /**
     * Gets sync me.
     *
     * @return the sync me
     */
    public SyncBool getSyncMe() {
        return mSyncMe;
    }

    /**
     * Gets sync me also.
     *
     * @return the sync me also
     */
    public SyncString getSyncMeAlso() {
        return mSyncMeAlso;
    }

    /** A syncable field. */
    SyncBool mSyncMe = new SyncBool(false);
    /** A syncable field. */
    SyncString mSyncMeAlso = new SyncString("Hello World");

    /**
     * Instantiates a new Capital.
     *
     * @param componentId the id
     */
    public Capital(int ownerId, int componentId) {
        super(ownerId,componentId);
    }

    /** Instantiates a new Capital without id, this will be set from the bytes */
    public Capital() {
        super();
    }

    /**
     * Modifies the boolean sync me.
     *
     * @param val the val
     */
    public void setBooleanSyncMe(boolean val) {
        this.mSyncMe.set(val);
    }

    /**
     * Modifies the string sync me also.
     *
     * @param val the val
     */
    public void setStringSyncMeAlso(String val) {
        this.mSyncMeAlso.set(val);
    }

    @Override
    protected void onDestroy() {}
}
