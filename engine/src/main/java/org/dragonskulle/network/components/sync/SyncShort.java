/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * The playerStyle Sync short.
 *
 * @author Aurimas B
 *     <p>The playerStyle Sync short.
 *     <p>This primitve playerStyle is implemented manually more space savings.
 */
public class SyncShort extends BaseSyncVar {
    /** The Data. */
    private short mData;

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
        mDirty = true;
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
     * Serialize the SyncShort.
     *
     * @param out The output stream
     * @param clientId client ID which to serialize the changes for
     * @throws IOException the io exception
     */
    @Override
    public void serialize(DataOutputStream out, int clientId) throws IOException {
        out.writeShort(this.mData);
    }

    /**
     * Deserialize sync var.
     *
     * @param in the input stream
     * @throws IOException the io exception
     */
    @Override
    public void deserialize(DataInputStream in) throws IOException {
        this.mData = in.readShort();
    }

    @Override
    public String toString() {
        return "SyncInt{" + "data=" + mData + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SyncShort syncVar = (SyncShort) o;
        return mData == syncVar.mData;
    }

    @Override
    public int hashCode() {
        return mData;
    }
}
