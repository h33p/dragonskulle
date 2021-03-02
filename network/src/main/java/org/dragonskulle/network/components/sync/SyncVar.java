/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.*;
import java.util.Objects;
import java.util.UUID;

/**
 * The type Sync var.
 *
 * @param <T> the type parameter
 */
public class SyncVar<T extends Serializable> implements Serializable {

    /**
     * The Data.
     */
    T data;
    /**
     * The Id.
     */
    final String id;
    /**
     * The Has listener.
     */
    private transient boolean hasListener = false;
    /**
     * The On update.
     */
    private transient ISyncVarUpdateHandler onUpdate;

    /**
     * Gets id.
     *
     * @return the id
     */
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

    /**
     * Instantiates a new Sync var.
     *
     * @param id   the id
     * @param data the data
     */
    public SyncVar(String id, T data) {
        this.id = id;
        this.data = data;
    }

    /**
     * Instantiates a new Sync var.
     *
     * @param data the data
     */
    public SyncVar(T data) {
        this.id = UUID.randomUUID().toString();
        this.data = data;
    }

    /**
     * Set.
     *
     * @param data the data
     */
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

    /**
     * Get t.
     *
     * @return the t
     */
    public T get() {
        return data;
    }

    /**
     * Serialize byte [ ].
     *
     * @return the byte [ ]
     * @throws IOException the io exception
     */
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        oos.flush();
        return bos.toByteArray();
    }

    /**
     * Deserialize sync var.
     *
     * @param buff the buff
     * @return the sync var
     * @throws IOException            the io exception
     * @throws ClassNotFoundException the class not found exception
     */
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

    /**
     * The interface Sync var update handler.
     */
    public interface ISyncVarUpdateHandler {
        /**
         * Call.
         */
        void call();
    }

    /**
     * Register listener.
     *
     * @param handleFieldChange the handle field change
     */
    public void registerListener(ISyncVarUpdateHandler handleFieldChange) {
        this.hasListener = true;
        this.onUpdate = handleFieldChange;
    }
}
