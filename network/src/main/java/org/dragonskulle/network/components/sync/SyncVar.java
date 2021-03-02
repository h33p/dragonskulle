/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.*;
import java.util.Objects;
import java.util.UUID;

public class SyncVar<T extends Serializable> implements Serializable {

    T data;
    final String id;
    private transient boolean hasListener = false;
    private transient ISyncVarUpdateHandler onUpdate;

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
        this.id = id;
        this.data = data;
    }

    public SyncVar(T data) {
        this.id = UUID.randomUUID().toString();
        this.data = data;
    }

    public void set(T data) {
        if (hasListener) {
            if (data != this.data) {
                this.onUpdate
                        .call(); // onUpdate callback is to set the mask bit on modification to the
                // field
            }
        }
        this.data = data;
    }

    public T get() {
        return data;
    }

    public byte[] serialize() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        oos.flush();
        return bos.toByteArray();
    }

    public static SyncVar deserialize(byte[] buff) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(buff);
        ObjectInput in = new ObjectInputStream(bis);
        SyncVar out = (SyncVar) in.readObject();
        in.close();
        return out;
    }

    @Override
    public String toString() {
        return "SyncVar{" + "data=" + data + ", id='" + id + '\'' + '}';
    }

    public interface ISyncVarUpdateHandler {
        void call();
    }

    public void registerListener(ISyncVarUpdateHandler handleFieldChange) {
        this.hasListener = true;
        this.onUpdate = handleFieldChange;
    }
}
