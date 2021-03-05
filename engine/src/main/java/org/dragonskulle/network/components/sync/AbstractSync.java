/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.Serializable;

/**
 * @author Oscar L The type Abstract sync. New Sync Types must extend this class.
 * @param <T> the type parameter
 */
abstract class AbstractSync<T extends Serializable> extends SyncVar<T> {
//
//    /**
//     * Instantiates a new Abstract sync.
//     *
//     * @param id the id
//     * @param data the data
//     */
//    public AbstractSync(String id, T data) {
//        super(id, data);
//    }

    /**
     * Instantiates a new Abstract sync.
     *
     * @param data the data
     */
    public AbstractSync(T data) {
        super(data);
    }
}
