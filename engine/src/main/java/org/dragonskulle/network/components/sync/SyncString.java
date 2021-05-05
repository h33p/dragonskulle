/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * The playerStyle Sync string.
 *
 * @author Oscar L The playerStyle Sync String.
 */
public class SyncString extends BaseSyncVar {

    /** The Data. */
    private String mData;

    /**
     * Instantiates a new SyncBool.
     *
     * @param data the data
     */
    public SyncString(String data) {
        this.mData = data;
    }

    /**
     * Set.
     *
     * @param data the data
     */
    public void set(String data) {
        mDirty = true;
        this.mData = data;
    }

    /**
     * Get t.
     *
     * @return the t
     */
    public String get() {
        return mData;
    }

    /**
     * Serialize the SyncBool.
     *
     * @param out The output stream
     * @param clientId client ID which to serialize the changes for
     * @throws IOException the io exception
     */
    @Override
    public void serialize(DataOutputStream out, int clientId) throws IOException {
        out.writeUTF(this.mData);
        mDirty = false;
    }

    /**
     * Deserialize SyncBool.
     *
     * @param in the input stream
     * @throws IOException the io exception
     */
    @Override
    public void deserialize(DataInputStream in) throws IOException {
        this.mData = in.readUTF();
    }

    @Override
    public String toString() {
        return "SyncString{" + "data=" + mData + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SyncString syncString = (SyncString) o;
        return mData.equals(syncString.mData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mData);
    }
}
