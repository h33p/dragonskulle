/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.*;
import java.io.Serializable;

/**
 * @author Aurimas B The type Sync int.
 *     <p>This primitve type is implemented manually more space savings
 */
public class SyncInt implements ISyncVar, Serializable {
    /** The Data. */
    private int mData;
    /** The On update. */
    private transient ISyncVarUpdateHandler mOnUpdate;

    /**
     * Instantiates a new Sync int.
     *
     * @param initialValue initial value
     */
    public SyncInt(int initialValue) {
        mData = initialValue;
    }

    /** Instantiates a new Sync int. */
    public SyncInt() {}

    /**
     * Set.
     *
     * @param data the data
     */
    public void set(int data) {
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
    public int get() {
        return mData;
    }

    /**
     * Serialize byte [ ].
     *
     * @return the byte [ ]
     * @throws IOException the io exception
     */
    public void serialize(ObjectOutputStream oos) throws IOException {
        oos.writeInt(this.mData);
    }

    /**
     * Deserialize sync var.
     *
     * @param buff the buff
     * @throws IOException the io exception
     * @throws ClassNotFoundException the class not found exception
     */
    public void deserialize(ObjectInputStream in) throws IOException, ClassNotFoundException {
        this.mData = in.readInt();
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
        SyncInt syncVar = (SyncInt) o;
        return mData == syncVar.mData;
    }

    @Override
    public int hashCode() {
        return mData;
    }
}
