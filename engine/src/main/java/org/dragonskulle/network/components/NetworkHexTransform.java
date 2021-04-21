/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.network.components.sync.SyncVector3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Networkable hex transform.
 *
 * @author Aurimas Blažulionis and Oscar L
 */
@Accessors(prefix = "m")
public class NetworkHexTransform extends NetworkableComponent implements IFixedUpdate {
    /** Synchronized axial coordinate. */
    @Getter private SyncVector3 mAxialCoordinate = new SyncVector3(new Vector3f(0, 0, 0));
    /** Internal transform reference. */
    private TransformHex mHexTransform;

    /** Whether height should be synchronized or not. */
    @Getter @Setter private boolean mSyncHeight = true;

    /** Constructor for {@link NetworkHexTransform}. */
    public NetworkHexTransform() {
        mAxialCoordinate.set(new Vector3f(0, 0, 0));
    }

    @Override
    protected void onDestroy() {}

    @Override
    public void beforeNetSerialize() {
        setHexPosition();
    }

    @Override
    public void afterNetUpdate() {
        setHexPosition();
    }

    @Override
    public void fixedUpdate(float deltaTime) {
        if (getNetworkObject().isServer()) {
            setHexPosition();
        }
    }

    /**
     * Set the hex position.
     *
     * <p>If on server, this will set {@link mAxialCoordinate}, if on client - this will set
     * transform's coordinates.
     */
    private void setHexPosition() {
        if (mHexTransform == null) {
            mHexTransform = getGameObject().getTransform(TransformHex.class);
        }

        if (mHexTransform != null) {
            if (getNetworkObject().isServer()) {
                Vector3f newPosition = mHexTransform.getLocalPosition(new Vector3f());
                if (mSyncHeight) {
                    newPosition.z = mHexTransform.getHeight();
                }
                if (!mAxialCoordinate.get().equals(newPosition)) {
                    mAxialCoordinate.set(newPosition);
                }
            } else {
                Vector3fc pos = mAxialCoordinate.get();
                mHexTransform.setPosition(pos.x(), pos.y());
                if (mSyncHeight) {
                    mHexTransform.setHeight(pos.z());
                }
            }
        }
    }

    @Override
    public String toString() {
        return "NetworkHexTransform{" + "mAxialCoordinate=" + mAxialCoordinate + '}';
    }
}
