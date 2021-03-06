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
     * @throws IOException the io exception
     */
    void serialize(ObjectOutputStream oos) throws IOException;

    /**
     * Deserialize sync var.
     *
     * @param bytes the input bytes
     * @return The number of bytes read
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