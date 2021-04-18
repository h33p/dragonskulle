/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

/**
 * The type Sync bool.
 *
 * @author Oscar L The type Sync bool.
 */
public class SyncBool implements ISyncVar, Serializable {

    /** The Data. */
    private boolean mData;
    /** The On update. */
    private transient ISyncVarUpdateHandler mOnUpdate;

    /**
     * Instantiates a new SyncBool.
     *
     * @param data the data
     */
    public SyncBool(boolean data) {
        this.mData = data;
    }

    /**
     * Set.
     *
     * @param data the data
     */
    public void set(boolean data) {
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
    public boolean get() {
        return mData;
    }

    /**
     * Serialize the SyncBool.
     *
     * @param out The output stream
     * @throws IOException the io exception
     */
    @Override
    public void serialize(DataOutputStream out) throws IOException {
        out.writeBoolean(this.mData);
    }

    /**
     * Deserialize SyncBool.
     *
     * @param in the input stream
     * @throws IOException the io exception
     */
    @Override
    public void deserialize(DataInputStream in) throws IOException {
        this.mData = in.readBoolean();
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
        return "SyncBool{" + "data=" + mData + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (this == o) {
            return true;
        }
        SyncBool syncBool = (SyncBool) o;
        return mData == syncBool.mData;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mData);
    }
}
