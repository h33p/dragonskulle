package org.dragonskulle.network.components;


import java.util.UUID;

public class ISyncVar<T> {

    public ISyncVar(T data) {
        this.data = data;
    }

    public T get() {
        return data;
    }

    public void set(T data) {
        this.data = data;
    }

    T data;
    final static String id = UUID.randomUUID().toString();
}
