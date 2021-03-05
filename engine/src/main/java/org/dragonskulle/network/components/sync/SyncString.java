/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.*;
import java.util.Objects;

/** @author Oscar L The type Sync String. */
public class SyncString implements ISyncVar {

    /** The Data. */
    private String mData;
    /** The On update. */
    private transient ISyncVarUpdateHandler mOnUpdate;

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
        if (mOnUpdate != null) {
            if (!data.equals(this.mData)) {
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
    public String get() {
        return mData;
    }

    /**
     * Serialize the SyncBool.
     *
     * @return the bytes
     * @throws IOException the io exception
     */
    @Override
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeUTF(this.mData);
        oos.flush();
        return bos.toByteArray();
    }

    /**
     * Deserialize SyncBool.
     *
     * @param buff the buff
     * @throws IOException the io exception
     */
    @Override
    public void deserialize(byte[] buff) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(buff);
        ObjectInput in = new ObjectInputStream(bis);
        this.mData = in.readUTF();
        in.close();
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
        return "SyncString{" + "data=" + mData + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SyncString syncString = (SyncString) o;
        return mData.equals(syncString.mData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mData);
    }
};
