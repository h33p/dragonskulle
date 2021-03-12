/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** @author Oscar L */
public class SyncVector3 implements ISyncVar {
    private Vector3f mData = new Vector3f();

    /** The On update. */
    private transient ISyncVarUpdateHandler mOnUpdate;

    /**
     * Serialize the SyncVector3.
     *
     * @throws IOException thrown if couldn't write to stream
     */
    @Override
    public void serialize(ObjectOutputStream oos) throws IOException {
        oos.writeFloat(mData.x);
        oos.writeFloat(mData.y);
        oos.writeFloat(mData.z);
    }

    /**
     * Deserialize the SyncVector3.
     *
     * @throws IOException thrown if couldn't read from stream
     */
    @Override
    public void deserialize(ObjectInputStream stream) throws IOException {
        mData.set(stream.readFloat(), stream.readFloat(), stream.readFloat());
    }

    /**
     * Register listener.
     *
     * @param handleFieldChange the handle field change
     */
    public void registerListener(ISyncVarUpdateHandler handleFieldChange) {
        this.mOnUpdate = handleFieldChange;
    }

    /**
     * Instantiates a new Sync Vector.
     *
     * @param initialValue initial value
     */
    public SyncVector3(Vector3f initialValue) {
        mData = initialValue;
    }

    /** Instantiates a new Sync Vector. */
    public SyncVector3() {}

    /**
     * Set.
     *
     * @param data the data
     */
    public void set(Vector3f data) {
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
    public Vector3fc get() {
        return mData;
    }
}
