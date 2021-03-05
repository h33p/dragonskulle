/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * @author Oscar L The type Abstract sync. New Sync Types must extend this class.
 * @param <T> the type parameter
 */
abstract class GenericSync<T extends Serializable> implements ISyncVar, Serializable {

    /** The Data. */
    private T mData;
    /** The On update. */
    private transient ISyncVarUpdateHandler mOnUpdate;

    /**
     * Instantiates a new Sync var.
     *
     * @param data the data
     */
    public GenericSync(T data) {
        this.mData = data;
    }

    /**
     * Set.
     *
     * @param data the data
     */
    public void set(T data) {
        if (mOnUpdate != null) {
            if (data != this.mData) {
                this.mOnUpdate
                        .call(); // onUpdate callback is to set the mask bit on modification to the
                // field
            }
        }
        this.mData = data;
    }

    /**
     * Get t.
     *
     * @return the t
     */
    public T get() {
        return mData;
    }

    /**
     * Serialize byte [ ].
     *
     * @return the byte [ ]
     * @throws IOException the io exception
     */
    public void serialize(ObjectOutputStream oos) throws IOException {
        oos.writeObject(this.mData);
    }

    /**
     * Deserialize sync var.
     *
     * @param in The input stream
     * @throws IOException the io exception
     * @throws ClassNotFoundException the class not found exception
     */
    @SuppressWarnings("unchecked")
    public void deserialize(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // TODO: handle this somehow
        this.mData = (T) in.readObject();
        in.close();
    }

    /**
     * Register listener.
     *
     * @param handleFieldChange the handle field change
     */
    public void registerListener(ISyncVarUpdateHandler handleFieldChange) {
        this.mOnUpdate = handleFieldChange;
    }

    @Override
    public String toString() {
        return "SyncVar{" + "data=" + mData + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenericSync<?> syncVar = (GenericSync<?>) o;
        return Objects.equals(mData, syncVar.mData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mData);
    }
}
