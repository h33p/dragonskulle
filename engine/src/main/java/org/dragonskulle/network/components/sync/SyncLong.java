/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

/**
 * The type Sync long.
 *
 * @author Oscar L The type Sync Long.
 */
public class SyncLong extends BaseSyncVar {

    /** The Data. */
    private long mData;

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
        mDirty = true;
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
     * @param out The output stream
     * @param clientId client ID which to serialize the changes for
     * @throws IOException the io exception
     */
    @Override
    public void serialize(DataOutput out, int clientId) throws IOException {
        out.writeLong(this.mData);
    }

    /**
     * Deserialize SyncBool.
     *
     * @param in the object stream
     * @throws IOException the io exception
     */
    @Override
    public void deserialize(DataInput in) throws IOException {
        this.mData = in.readLong();
    }

    @Override
    public String toString() {
        return "SyncLong{" + "data=" + mData + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SyncLong syncLong = (SyncLong) o;
        return mData == syncLong.mData;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mData);
    }
}
