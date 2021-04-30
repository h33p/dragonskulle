/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * The type Sync int.
 *
 * @author Aurimas B The type Sync int.
 *     <p>This primitve type is implemented manually more space savings.
 */
public class SyncInt extends BaseSyncVar {
    /** The Data. */
    private int mData;

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
        mDirty = true;
        this.mData = data;
    }

    /**
     * Add a value to the current data.
     *
     * @param value The value to add.
     */
    public void add(int value) {
        set(get() + value);
    }

    /**
     * Subtract a value from the current data.
     *
     * @param value The value to subtract.
     */
    public void subtract(int value) {
        set(get() - value);
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
     * Serialize bytes.
     *
     * @param out The output stream
     * @param clientId client ID which to serialize the changes for
     * @throws IOException the io exception
     */
    public void serialize(DataOutputStream out, int clientId) throws IOException {
        out.writeInt(this.mData);
    }

    /**
     * Deserialize sync var.
     *
     * @param in the input stream
     * @throws IOException the io exception
     */
    public void deserialize(DataInputStream in) throws IOException {
        this.mData = in.readInt();
    }

    @Override
    public String toString() {
        return "SyncInt{" + "data=" + '\'' + mData + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SyncInt syncVar = (SyncInt) o;
        return mData == syncVar.mData;
    }

    @Override
    public int hashCode() {
        return mData;
    }
}
