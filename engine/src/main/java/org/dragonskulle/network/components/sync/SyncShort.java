/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.*;
import java.io.Serializable;

/**
 * @author Aurimas B The type Sync short.
 *     <p>This primitve type is implemented manually more space savings
 */
public class SyncShort implements ISyncVar, Serializable {
    /** The Data. */
    private short mData;
    /** The On update. */
    private transient ISyncVarUpdateHandler mOnUpdate;

    /**
     * Instantiates a new Sync short.
     *
     * @param initialValue initial value
     */
    public SyncShort(short initialValue) {
        mData = initialValue;
    }

    /** Instantiates a new Sync short. */
    public SyncShort() {}

    /**
     * Set.
     *
     * @param data the data
     */
    public void set(short data) {
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
     * Get short value.
     *
     * @return the value
     */
    public short get() {
        return mData;
    }

    /**
     * Serialize byte [ ].
     *
     * @return the byte [ ]
     * @throws IOException the io exception
     */
    public void serialize(ObjectOutputStream oos) throws IOException {
        oos.writeShort(this.mData);
    }

    /**
     * Deserialize sync var.
     *
     * @param buff the buff
     * @throws IOException the io exception
     * @throws ClassNotFoundException the class not found exception
     */
    public void deserialize(ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.mData = in.readShort();
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
        return "SyncInt{" + "data=" + mData + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SyncShort syncVar = (SyncShort) o;
        return mData == syncVar.mData;
    }

    @Override
    public int hashCode() {
        return mData;
    }
}
