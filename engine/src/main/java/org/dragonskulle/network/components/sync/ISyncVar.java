/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

/**
 * The interface Sync var.
 *
 * @author Oscar L The type Sync var.
 */
public interface ISyncVar extends INetSerializable {
    /**
     * Check if object is dirty for a given player.
     *
     * @param clientId client to check the dirtiness for
     * @return whether this syncvar is dirty and should be synchronized for the player
     */
    boolean isDirty(int clientId);

    /** Resets the global dirty flag. */
    default void resetDirtyFlag() {}

    /**
     * Resets the dirty flag for a given client.
     *
     * @param clientId id of the client to reset the mask for
     */
    default void resetDirtyFlag(int clientId) {}

    /**
     * Sets is a flag on the client that the variable is dirty.
     *
     * @param isDirty true if to be dirty
     */
    default void setIsClientDirty(boolean isDirty) {}
}
