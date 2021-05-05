/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * The playerStyle Sync vector 3.
 *
 * @author Oscar L
 */
public class SyncVector3 extends BaseSyncVar {
    private Vector3f mData = new Vector3f();

    /**
     * Serialize the SyncVector3.
     *
     * @param out The output stream
     * @param clientId client ID which to serialize the changes for
     * @throws IOException thrown if couldn't write to stream
     */
    @Override
    public void serialize(DataOutputStream out, int clientId) throws IOException {
        out.writeFloat(mData.x);
        out.writeFloat(mData.y);
        out.writeFloat(mData.z);
        mDirty = false;
    }

    /**
     * Deserialize the SyncVector3.
     *
     * @throws IOException thrown if couldn't read from stream
     */
    @Override
    public void deserialize(DataInputStream stream) throws IOException {
        mData.set(stream.readFloat(), stream.readFloat(), stream.readFloat());
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
    public void set(Vector3fc data) {
        mDirty = true;
        this.mData.set(data);
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
