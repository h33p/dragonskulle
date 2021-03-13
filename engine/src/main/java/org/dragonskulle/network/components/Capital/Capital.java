/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.Capital;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.network.components.NetworkableComponent;
import org.dragonskulle.network.components.sync.SyncBool;
import org.dragonskulle.network.components.sync.SyncString;

/** @author Oscar L The Capital Component. */
@Accessors(prefix = "m")
public class Capital extends NetworkableComponent {

    /** A syncable field. */
    @Getter public SyncBool mSyncMe = new SyncBool(false);
    /** A syncable field. */
    @Getter public SyncString mSyncMeAlso = new SyncString("Hello World");

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
