/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * The type Sync float.
 *
 * @author Oscar L
 */
public class SyncFloat extends BaseSyncVar {

    private float mData;

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
        mDirty = true;
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
     * @param out The output stream
     * @param clientId client ID which to serialize the changes for
     * @throws IOException the io exception
     */
    public void serialize(DataOutputStream out, int clientId) throws IOException {
        out.writeFloat(this.mData);
    }

    /**
     * Deserialize sync var.
     *
     * @param in the input stream
     * @throws IOException the io exception
     */
    public void deserialize(DataInputStream in) throws IOException {
        this.mData = in.readFloat();
    }

    @Override
    public String toString() {
        return "SyncFloat{" + "data=" + mData + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SyncFloat syncVar = (SyncFloat) o;
        return mData == syncVar.mData;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mData);
    }
}
