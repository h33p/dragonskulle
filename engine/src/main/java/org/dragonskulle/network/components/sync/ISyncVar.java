/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

/**
 * The interface Sync var.
 *
 * @author Oscar L The type Sync var.
 */
public interface ISyncVar extends INetSerializable {
    /**
     * Check if object is dirty for a given player
     *
     * @param clientId client to check the dirtiness for
     */
    boolean isDirty(int clientId);

    /** Resets the dirty flag */
    void resetDirtyFlag();
}
