package org.dragonskulle.network.components.sync;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class SyncVar<T extends Serializable> implements Serializable {

    T data;
    final String id;

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        SyncVar<?> syncVar = (SyncVar<?>) o;
        if (getId().equals(syncVar.getId())) return true;
        if (getClass() != o.getClass()) return false;
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }


    public SyncVar(String id, T data) {
        this.data = data;
        this.id = id;
    }

    public SyncVar(T data) {
        this.id = UUID.randomUUID().toString();
        this.data = data;
    }

    void set(T data) {
//        if (onUpdate != null) {
        System.out.println("Setting var :: " + getId());
        this.data = data;
//        }
    }

    T get() {
        return data;
    }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        oos.flush();
        return bos.toByteArray();
    }
}
