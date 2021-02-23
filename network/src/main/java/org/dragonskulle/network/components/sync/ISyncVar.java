package org.dragonskulle.network.components.sync;

import java.util.Objects;
import java.util.UUID;

public class ISyncVar<T> {

    T data;
    final String id;

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        ISyncVar<?> iSyncVar = (ISyncVar<?>) o;
        if (getId().equals(iSyncVar.getId())) return true;
        if (getClass() != o.getClass()) return false;
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }


    public ISyncVar(String id, T data) {
        this.data = data;
        this.id = id;
    }

    public ISyncVar(T data) {
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

}
