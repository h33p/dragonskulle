/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.Serializable;

abstract class AbstractSync<T extends Serializable> extends SyncVar<T> {

    public AbstractSync(String id, T data) {
        super(id, data);
    }

    public AbstractSync(T data) {
        super(data);
    }
}
