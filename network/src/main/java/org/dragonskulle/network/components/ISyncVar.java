package org.dragonskulle.network.components;


import com.google.flatbuffers.FlatBufferBuilder;

import java.util.UUID;

public class ISyncVar<T> {


    public ISyncVar(String id, T data) {
        this.data = data;
        this.id = id;
    }


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

    byte[] serialize() {
        return null;
    }
}
