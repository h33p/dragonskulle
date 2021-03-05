/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.*;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Oscar L The type Sync var.
 * @param <T> the type parameter
 */
public class SyncVar<T extends Serializable> implements Serializable {
    /** The Data. */
    private T mData;
//    /** The Id. */
//    private final String mId;
    /** The Has listener. */
    private transient boolean mHasListener = false;
    /** The On update. */
    private transient ISyncVarUpdateHandler mOnUpdate;

//    /**
//     * Gets id.
//     *
//     * @return the id
//     */
//    public String getId() {
//        return mId;
//    }
//
//
//    /**
//     * Instantiates a new Sync var.
//     *
//     * @param id the id
//     * @param data the data
//     */
//    public SyncVar(String id, T data) {
//        this.mId = id;
//        this.mData = data;
//    }

    /**
     * Instantiates a new Sync var.
     *
     * @param data the data
     */
    public SyncVar(T data) {
//        this.mId = UUID.randomUUID().toString();
        this.mData = data;
    }

    /**
     * Set.
     *
     * @param data the data
     */
    public void set(T data) {
        if (mHasListener) {
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
    public T get() {
        return mData;
    }

    /**
     * Serialize byte [ ].
     *
     * @return the byte [ ]
     * @throws IOException the io exception
     */
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        oos.flush();
        return bos.toByteArray();
    }

    /**
     * Deserialize sync var.
     *
     * @param buff the buff
     * @return the sync var
     * @throws IOException the io exception
     * @throws ClassNotFoundException the class not found exception
     */
    public static SyncVar deserialize(byte[] buff) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(buff);
        ObjectInput in = new ObjectInputStream(bis);
        SyncVar out = (SyncVar) in.readObject();
        in.close();
        return out;
    }

    @Override
    public String toString() {
        return "SyncVar{" + "data=" + mData + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SyncVar<?> syncVar = (SyncVar<?>) o;
        return mData.equals(syncVar.mData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mData);
    }

    /** The interface Sync var update handler. */
    public interface ISyncVarUpdateHandler {
        /** Call. */
        void call();
    }

    /**
     * Register listener.
     *
     * @param handleFieldChange the handle field change
     */
    public void registerListener(ISyncVarUpdateHandler handleFieldChange) {
        this.mHasListener = true;
        this.mOnUpdate = handleFieldChange;
    }
}
