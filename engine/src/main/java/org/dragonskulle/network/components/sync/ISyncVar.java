/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.*;

/** @author Oscar L The type Sync var. */
public interface ISyncVar {

    /** The interface Sync var update handler. */
    public interface ISyncVarUpdateHandler {
        /** Call. */
        void call();
    }

    /**
     * Serialize Sync Var.
     *
     * @return the byte [ ]
     * @throws IOException the io exception
     */
    public void serialize(ObjectOutputStream oos) throws IOException;

    /**
     * Deserialize sync var.
     *
     * @param buff the buff
     * @throws IOException the io exception
     * @throws ClassNotFoundException the class not found exception
     */
    void deserialize(ObjectInputStream stream) throws IOException, ClassNotFoundException;

    /**
     * Register listener.
     *
     * @param handleFieldChange the handle field change
     */
    public void registerListener(ISyncVarUpdateHandler handleFieldChange);
}
