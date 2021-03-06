/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.*;
import java.util.Objects;

/** @author Oscar L The type Sync Long. */
public class SyncLong implements ISyncVar {

    /** The Data. */
    private long mData;
    /** The On update. */
    private transient ISyncVarUpdateHandler mOnUpdate;

    /**
     * Instantiates a new SyncBool.
     *
     * @param data the data
     */
    public SyncLong(long data) {
        this.mData = data;
    }

    /**
     * Set.
     *
     * @param data the data
     */
    public void set(long data) {
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
    public long get() {
        return mData;
    }

    /**
     * Serialize the SyncLong.
     *
     * @return the bytes
     * @throws IOException the io exception
     */
    @Override
    public void serialize(ObjectOutputStream oos) throws IOException {
        oos.writeLong(this.mData);
    }

    /**
     * Deserialize SyncBool.
     *
     * @param in the object stream
     * @throws IOException the io exception
     */
    @Override
    public void deserialize(ObjectInputStream in) throws IOException {
        this.mData = in.readLong();
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
        return "SyncLong{" + "data=" + mData + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SyncLong syncLong = (SyncLong) o;
        return mData == syncLong.mData;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mData);
    }
};
