/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

/**
 * The interface Sync var.
 *
 * @author Oscar L The type Sync var.
 */
public interface ISyncVar extends INetSerializable {
    /** The interface Sync var update handler. */
    interface ISyncVarUpdateHandler {
        /** Call. */
        void call();
    }

    /**
     * Register listener.
     *
     * @param handleFieldChange the handle field change
     */
    void registerListener(ISyncVarUpdateHandler handleFieldChange);
}
