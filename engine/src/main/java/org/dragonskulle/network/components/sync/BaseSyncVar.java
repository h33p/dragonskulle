/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

/**
 * Base syncvar with dirty flag
 *
 * @author Aurimas Blažulionis
 */
public abstract class BaseSyncVar implements ISyncVar {

    /** Is set whenever the extending SyncVar's value changes. */
    protected boolean mDirty;

    /**
     * Check if object is dirty for a given player.
     *
     * @param clientId client to check the dirtiness for
     */
    @Override
    public boolean isDirty(int clientId) {
        return mDirty;
    }

    @Override
    public void resetDirtyFlag() {
        mDirty = false;
    }
}
