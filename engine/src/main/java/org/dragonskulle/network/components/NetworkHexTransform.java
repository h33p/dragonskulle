/* (C) 2021 DragonSkulle */

package org.dragonskulle.network.components;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.dragonskulle.components.IFixedUpdate;
import org.dragonskulle.components.TransformHex;
import org.dragonskulle.network.components.sync.SyncVector3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@Accessors(prefix = "m")
public class NetworkHexTransform extends NetworkableComponent implements IFixedUpdate {
    @Getter private SyncVector3 mAxialCoordinate = new SyncVector3(new Vector3f(0, 0, 0));
    private TransformHex mHexTransform;

    public NetworkHexTransform(int q, int r) {
        mAxialCoordinate.set(new Vector3f(q, r, 0));
    }

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
        if (getNetworkObject().isServer()) setHexPosition();
    }

    private void setHexPosition() {
        if (mHexTransform == null) mHexTransform = getGameObject().getTransform(TransformHex.class);

        if (mHexTransform != null) {
            if (getNetworkObject().isServer()) {
                Vector3f newPosition = mHexTransform.getLocalPosition(new Vector3f());
                newPosition.z = mHexTransform.getHeight();
                if (!mAxialCoordinate.get().equals(newPosition)) {
                    mAxialCoordinate.set(newPosition);
                }
            } else {
                Vector3fc pos = mAxialCoordinate.get();
                mHexTransform.setPosition(pos.x(), pos.y());
                mHexTransform.setHeight(pos.z());
            }
        }
    }

    @Override
    public String toString() {
        return "NetworkHexTransform{" + "mAxialCoordinate=" + mAxialCoordinate + '}';
    }
}
