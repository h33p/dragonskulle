/* (C) 2021 DragonSkulle */
package org.dragonskulle.network.components;

import org.dragonskulle.components.*;
import org.dragonskulle.network.components.sync.SyncVector3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class NetworkHexTransform extends NetworkableComponent implements IFixedUpdate {
    public SyncVector3 mAxialCoordinate = new SyncVector3(new Vector3f(0, 0, 0));
    private TransformHex hexTransform;

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
        if (hexTransform == null) hexTransform = getGameObject().getTransform(TransformHex.class);

        if (hexTransform != null) {
            if (getNetworkObject().isServer()) {
                Vector3f newPosition = hexTransform.getLocalPosition(new Vector3f());
                newPosition.z = hexTransform.getHeight();
                if (!mAxialCoordinate.get().equals(newPosition)) {
                    mAxialCoordinate.set(newPosition);
                }
            } else {
                Vector3fc pos = mAxialCoordinate.get();
                hexTransform.setPosition(pos.x(), pos.y());
                hexTransform.setHeight(pos.z());
            }
        }
    }

    @Override
    public String toString() {
        return "NetworkHexTransform{" + "mAxialCoordinate=" + mAxialCoordinate + '}';
    }
}
