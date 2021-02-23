package org.dragonskulle.network.components.sync;


abstract class AbstractSync<T> extends ISyncVar<T> {

    public AbstractSync(String id, T data) {
        super(id, data);
    }

    public AbstractSync(T data) {
        super(data);
    }
}