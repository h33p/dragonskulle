/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;

/** @author Oscar L */
public class SyncFloat implements ISyncVar, Serializable {

    private float mData;
    /** The On update. */
    private transient ISyncVarUpdateHandler mOnUpdate;

    /**
     * Instantiates a new Sync float.
     *
     * @param initialValue initial value
     */
    public SyncFloat(float initialValue) {
        mData = initialValue;
    }

    /** Instantiates a new Sync float. */
    public SyncFloat() {}

    /**
     * Set.
     *
     * @param data the data
     */
    public void set(float data) {
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
     * Get integer value.
     *
     * @return the value
     */
    public float get() {
        return mData;
    }

    /**
     * Serialize bytes.
     *
     * @param oos The output stream
     * @throws IOException the io exception
     */
    public void serialize(ObjectOutputStream oos) throws IOException {
        oos.writeFloat(this.mData);
    }

    /**
     * Deserialize sync var.
     *
     * @param in the input stream
     * @throws IOException the io exception
     */
    public void deserialize(ObjectInputStream in) throws IOException {
        this.mData = in.readFloat();
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
        return "SyncFloat{" + "data=" + mData + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SyncFloat syncVar = (SyncFloat) o;
        return mData == syncVar.mData;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mData);
    }
}
